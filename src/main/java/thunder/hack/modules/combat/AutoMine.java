package thunder.hack.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thunder.hack.ThunderHack;
import thunder.hack.core.impl.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.modules.Module;
import thunder.hack.modules.player.SpeedMine;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Parent;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.world.HoleUtility;

import static thunder.hack.modules.client.MainSettings.isRu;

public final class AutoMine extends Module {
    private final Setting<Float> range = new Setting<>("Range", 5f, 1f, 7f);
    private final Setting<Boolean> autoDisable = new Setting<>("Auto Disable", true);
    private final Setting<Boolean> autoSwitch = new Setting<>("Switch", true);
    private final Setting<Boolean> requirePickaxe = new Setting<>("Only Pickaxe", false);
    private final Setting<Boolean> oldVers = new Setting<>("1.12 Mode", false);
    private final Setting<Boolean> rotate = new Setting<>("Rotate", false);
    private final Setting<Boolean> swing = new Setting<>("Swing", false);

    private final Setting<Parent> logic = new Setting<>("Logic", new Parent(false, 0));
    private final Setting<TargetLogic> targetLogic = new Setting<>("TargetLogic", TargetLogic.Distance).withParent(logic);
    private final Setting<Boolean> antiBurrow = new Setting<>("Burrow", false).withParent(logic);
    private final Setting<Boolean> antiSurround = new Setting<>("Surround", true).withParent(logic);
    private final Setting<Boolean> antiCev = new Setting<>("Cev", true).withParent(logic);
    private final Setting<Boolean> antiCiv = new Setting<>("Civ", true).withParent(logic);

    private static AutoMine instance;

    private BlockPos blockPos;

    private enum TargetLogic {
        Distance,
        HP,
        FOV
    }

    public AutoMine() {
        super("AutoMine", Category.COMBAT);
        instance = this;
    }

    @Override
    public void onEnable() {
        blockPos = null;
    }

    @EventHandler
    public void onSync(EventSync event) {
        if (fullNullCheck()) return;
        if (requirePickaxe.getValue() && !checkPickaxe()) return;

        if (blockPos != null && mc.world.isAir(blockPos)) {
            if (autoDisable.getValue()) {
                disable(isRu() ? "Сарраунд пробит! Отключаю..." : "Surround has been broken! Turning off...");
                return;
            }
            blockPos = null;
        }

        BlockPos minePos = null;
        PlayerEntity target = null;
        boolean found = false;

        switch (targetLogic.getValue()) {
            case HP -> target = ThunderHack.combatManager.getTargetByHealth(range.getValue() + 1f);
            case Distance -> target = ThunderHack.combatManager.getNearestTarget(range.getValue() + 1f);
            case FOV -> target = ThunderHack.combatManager.getTargetByFOV(range.getValue() + 1f);
        }

        if (target == null) return;

        BlockPos targetPos = BlockPos.ofFloored(target.getPos());

        if (!HoleUtility.isHole(targetPos)) return;

        // 1 прогон - на буров
        if (antiBurrow.getValue() && !mc.world.getBlockState(targetPos).isReplaceable()) {
            minePos = targetPos;
            found = true;
        }

        // 2 прогон - ищем на которые можно поставить
        if (antiSurround.getValue() && !found) {
            for (BlockPos offset : HoleUtility.getSurroundPoses(target.getPos())) {
                if (mc.world.getBlockState(offset).getBlock() != Blocks.OBSIDIAN)
                    continue;
                if (!mc.world.isAir(offset.add(0, 1, 0)))
                    continue;
                if (mc.player.squaredDistanceTo(offset.toCenterPos()) < range.getPow2Value()) {
                    minePos = offset;
                    found = true;
                    break;
                }
            }
        }

        // 3 прогон кив брейкаем
        if (!found && antiCiv.getValue()) {
            for (BlockPos pos : HoleUtility.getSurroundPoses(target.getPos()).stream()
                    .map(BlockPos::up)
                    .toList()) {
                if (!mc.world.getBlockState(pos).getBlock().equals(Blocks.OBSIDIAN) || !mc.world.isAir(pos)) continue;
                if (mc.player.squaredDistanceTo(pos.toCenterPos()) < range.getPow2Value()) {
                    minePos = pos;
                    found = true;
                    break;
                }
            }
        }

        // 4 прогон кев брейкаем
        if (!found
                && antiCev.getValue()
                && mc.world.getBlockState(targetPos.up(2)).getBlock().equals(Blocks.OBSIDIAN)
                && mc.world.getBlockState(targetPos.up(3)).isAir()) {
            minePos = targetPos.up(2);
            found = true;
        }

        // 5 прогон - ищем любой
        if (!found && !oldVers.getValue() && antiSurround.getValue()) {
            for (BlockPos pos2 : HoleUtility.getSurroundPoses(target.getPos())) {
                if (mc.world.getBlockState(pos2).getBlock() != Blocks.OBSIDIAN)
                    continue;
                if (mc.player.squaredDistanceTo(pos2.toCenterPos()) < range.getPow2Value()) {
                    minePos = pos2;
                    break;
                }
            }
        }

        if (minePos != null) {
            if (autoSwitch.getValue())
                InventoryUtility.getPickAxe().switchTo();

            if (rotate.getValue()) {
                float[] rotation = getRotations(minePos);
                if (rotation != null) {
                    mc.player.setYaw(rotation[0]);
                    mc.player.setPitch(rotation[1]);
                }
            }

            if (!requirePickaxe.getValue() || mc.player.getMainHandStack().getItem() instanceof PickaxeItem) {
                if (ModuleManager.speedMine.isEnabled()
                        && (SpeedMine.minePosition != null || SpeedMine.progress != 0))
                    return;
                InteractionUtility.BreakData data = InteractionUtility.getBreakData(minePos, InteractionUtility.Interact.Strict);
                if (data == null)
                    return;

                if (ModuleManager.speedMine.isEnabled()) mc.interactionManager.attackBlock(minePos, data.dir());
                else mc.interactionManager.updateBlockBreakingProgress(minePos, data.dir());

                if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
                blockPos = minePos;
            }
        }
    }

    public static float @Nullable [] getRotations(@NotNull BlockPos blockPos) {
        InteractionUtility.BreakData data = InteractionUtility.getBreakData(blockPos, InteractionUtility.Interact.Strict);
        return data == null ? null : InteractionUtility.calculateAngle(data.vector());
    }

    public boolean checkPickaxe() {
        return mc.player.getMainHandStack().getItem() instanceof PickaxeItem;
    }

    public static AutoMine getInstance() {
        return instance;
    }
}
