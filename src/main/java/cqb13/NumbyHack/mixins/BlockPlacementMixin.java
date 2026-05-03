package cqb13.NumbyHack.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cqb13.NumbyHack.modules.general.CarpetPlacer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
public class BlockPlacementMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void preventBlockPlacement(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        CarpetPlacer carpetPlacer = Modules.get().get(CarpetPlacer.class);

        if (carpetPlacer == null || !carpetPlacer.isActive() || !carpetPlacer.isAntiStackEnabled()) {
            return;
        }

        Level world = player.level();
        BlockPos hitPos = hitResult.getBlockPos();
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.getItem() instanceof BlockItem) {
            BlockPos placePos = hitPos.relative(hitResult.getDirection());
            BlockPos belowPlacePos = placePos.below();
            BlockState blockBelow = world.getBlockState(belowPlacePos);

            if (shouldPreventPlacement(itemStack, blockBelow, carpetPlacer)) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }

    private boolean shouldPreventPlacement(ItemStack itemStack, BlockState blockBelow, CarpetPlacer carpetPlacer) {
        Block itemBlock = ((BlockItem) itemStack.getItem()).getBlock();
        Block belowBlock = blockBelow.getBlock();

        if (itemBlock instanceof CarpetBlock && belowBlock instanceof CarpetBlock) {
            return true;
        }

        return false;
    }
}
