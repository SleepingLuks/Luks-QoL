package luks.qol.mixin;

import luks.qol.trade.VillagerTradeHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Обновление торговли (6 секунд) и сброс профессии (5 минут) для жителя.
 * Всё состояние и таймеры живут на сервере; окно торговли только показывает их и шлёт нажатия.
 * Таймеры хранят игровое время окончания, поэтому не обнуляются при повторном открытии окна или перезаходе.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements VillagerTradeHolder {
    @Unique
    private static final long LUKSQOL$REFRESH_TICKS = 3L * 20L;
    @Unique
    private static final long LUKSQOL$RESET_TICKS = 5L * 60L * 20L;

    /** Была ли успешная сделка. */
    @Unique
    private boolean luksqol$traded = false;
    /** Игровое время, когда обновятся предложения (0 — таймер не идёт). */
    @Unique
    private long luksqol$refreshEnd = 0L;
    /** Игровое время, когда сбросится профессия (0 — таймер не идёт). */
    @Unique
    private long luksqol$resetEnd = 0L;

    protected VillagerMixin(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract VillagerData getVillagerData();

    @Shadow
    public abstract void setVillagerData(VillagerData data);

    @Shadow
    public abstract void refreshBrain(ServerLevel level);

    @Shadow
    public abstract void releasePoi(MemoryModuleType<net.minecraft.core.GlobalPos> type);

    @Shadow
    protected abstract void updateSpecialPrices(Player player);

    // ---- состояние для окна ----

    @Override
    public boolean luksqol$isSupported() {
        var profession = this.getVillagerData().profession();
        return !profession.is(VillagerProfession.NONE) && !profession.is(VillagerProfession.NITWIT);
    }

    @Override
    public boolean luksqol$hasTraded() {
        // Опыт > 0 — запасной признак для жителей, торговавших до установки мода.
        return this.luksqol$traded || this.getVillagerXp() > 0;
    }

    @Override
    public int luksqol$refreshSeconds() {
        return this.luksqol$secondsLeft(this.luksqol$refreshEnd);
    }

    @Override
    public int luksqol$resetSeconds() {
        return this.luksqol$secondsLeft(this.luksqol$resetEnd);
    }

    @Unique
    private int luksqol$secondsLeft(long end) {
        if (end <= 0L) {
            return 0;
        }
        long left = end - this.level().getGameTime();
        return left <= 0L ? 0 : (int) ((left + 19L) / 20L);
    }

    // ---- действия игрока (вызываются только на сервере) ----

    @Override
    public boolean luksqol$startRefresh() {
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        // Пока идёт сброс профессии, обновлять предложения бессмысленно.
        if (!this.luksqol$isSupported() || this.luksqol$hasTraded()
                || this.luksqol$refreshEnd != 0L || this.luksqol$resetEnd != 0L) {
            return false;
        }
        this.luksqol$refreshEnd = level.getGameTime() + LUKSQOL$REFRESH_TICKS;
        return true;
    }

    @Override
    public boolean luksqol$startReset() {
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!this.luksqol$isSupported() || this.luksqol$hasTraded() || this.luksqol$resetEnd != 0L) {
            return false;
        }
        this.luksqol$resetEnd = level.getGameTime() + LUKSQOL$RESET_TICKS;
        return true;
    }

    @Override
    public boolean luksqol$cancelReset() {
        if (this.luksqol$resetEnd == 0L) {
            return false;
        }
        this.luksqol$resetEnd = 0L;
        return true;
    }

    // ---- первая сделка закрывает обе функции ----

    @Inject(method = "rewardTradeXp", at = @At("HEAD"))
    private void luksqol$onFirstTrade(MerchantOffer offer, CallbackInfo ci) {
        this.luksqol$traded = true;
        this.luksqol$refreshEnd = 0L;
        this.luksqol$resetEnd = 0L;
    }

    // ---- таймеры ----

    @Inject(method = "tick", at = @At("TAIL"))
    private void luksqol$tickTimers(CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.luksqol$refreshEnd == 0L && this.luksqol$resetEnd == 0L) {
            return;
        }
        if (this.luksqol$hasTraded()) {
            this.luksqol$refreshEnd = 0L;
            this.luksqol$resetEnd = 0L;
            return;
        }
        long now = level.getGameTime();
        if (this.luksqol$resetEnd != 0L && now >= this.luksqol$resetEnd) {
            this.luksqol$finishReset(level);
        } else if (this.luksqol$refreshEnd != 0L && now >= this.luksqol$refreshEnd) {
            this.luksqol$finishRefresh(level);
        }
    }

    @Unique
    private void luksqol$finishRefresh(ServerLevel level) {
        this.luksqol$refreshEnd = 0L;
        if (!this.luksqol$isSupported()) {
            return;
        }

        // Новый набор предложений по обычным правилам профессии и уровня жителя.
        this.offers = new MerchantOffers();
        this.updateTrades(level);

        Player trading = this.getTradingPlayer();
        if (trading != null) {
            this.updateSpecialPrices(trading); // скидки репутации для новых предложений
        }
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);

        if (trading instanceof ServerPlayer player && player.containerMenu instanceof MerchantMenu menu) {
            // Сбрасываем выбранную сделку и пересчитываем результат, чтобы в слоте не осталось предмета старой сделки.
            menu.setSelectionHint(0);
            menu.updateSellItem();
            player.sendMerchantOffers(menu.containerId, this.getOffers(), this.getVillagerData().level(),
                    this.getVillagerXp(), this.showProgressBar(), this.canRestock());
        }
    }

    @Unique
    private void luksqol$finishReset(ServerLevel level) {
        this.luksqol$resetEnd = 0L;
        this.luksqol$refreshEnd = 0L;
        if (!this.luksqol$isSupported()) {
            return;
        }

        // Окно торговли закрываем до смены профессии: безработный житель торговать не может.
        if (this.getTradingPlayer() instanceof ServerPlayer player) {
            player.closeContainer();
        }

        this.releasePoi(MemoryModuleType.JOB_SITE);
        this.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
        this.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        this.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);

        // Профессия сбрасывается как у ванильного жителя, потерявшего рабочее место; setVillagerData
        // сам сбрасывает набор предложений при смене профессии.
        this.setVillagerData(this.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NONE));
        this.refreshBrain(level);
        this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
    }

    // ---- сохранение ----

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void luksqol$saveTradeState(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("LuksTraded", this.luksqol$traded);
        output.putLong("LuksRefreshEnd", this.luksqol$refreshEnd);
        output.putLong("LuksResetEnd", this.luksqol$resetEnd);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void luksqol$loadTradeState(ValueInput input, CallbackInfo ci) {
        this.luksqol$traded = input.getBooleanOr("LuksTraded", false);
        this.luksqol$refreshEnd = input.getLongOr("LuksRefreshEnd", 0L);
        this.luksqol$resetEnd = input.getLongOr("LuksResetEnd", 0L);
    }
}
