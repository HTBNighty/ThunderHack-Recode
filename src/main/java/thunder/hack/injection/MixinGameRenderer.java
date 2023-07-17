package thunder.hack.injection;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.item.SwordItem;
import net.minecraft.resource.ResourceFactory;
import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import thunder.hack.Thunderhack;
import thunder.hack.events.impl.PreRender3DEvent;
import thunder.hack.events.impl.Render3DEvent;
import thunder.hack.modules.combat.Aura;
import thunder.hack.modules.player.NoEntityTrace;
import thunder.hack.modules.render.NoRender;
import thunder.hack.utility.math.FrameRateCounter;
import thunder.hack.utility.render.GlProgram;
import thunder.hack.utility.render.MSAAFramebuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gl.ShaderStage;
import thunder.hack.utility.render.Render3DEngine;

import java.util.List;
import java.util.function.Consumer;

import static thunder.hack.utility.Util.mc;


@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {


    @Shadow public abstract void render(float tickDelta, long startTime, boolean tick);

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", shift = At.Shift.BEFORE), method = "render")
    void postHudRenderHook(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        FrameRateCounter.INSTANCE.recordFrame();
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), method = "renderWorld")
    void render3dHook(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        Render3DEngine.lastProjMat.set(RenderSystem.getProjectionMatrix());
        Render3DEngine.lastModMat.set(RenderSystem.getModelViewMatrix());
        Render3DEngine.lastWorldSpaceMatrix.set(matrix.peek().getPositionMatrix());
        Thunderhack.EVENT_BUS.post(new PreRender3DEvent(matrix));
        MSAAFramebuffer.use(() -> Thunderhack.EVENT_BUS.post(new Render3DEvent(matrix)));
    }


    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (Thunderhack.moduleManager.get(NoRender.class).isEnabled() && Thunderhack.moduleManager.get(NoRender.class).hurtCam.getValue()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
        if (Thunderhack.moduleManager.get(NoRender.class).isEnabled() && Thunderhack.moduleManager.get(NoRender.class).nausea.getValue()) return 0;
        return MathHelper.lerp(delta, first, second);
    }

    @Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
        if (Thunderhack.moduleManager.get(NoEntityTrace.class).isEnabled() && (mc.player.getMainHandStack().getItem() instanceof PickaxeItem || !NoEntityTrace.ponly.getValue()) && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
           if(mc.player.getMainHandStack().getItem() instanceof SwordItem && NoEntityTrace.noSword.getValue()) return;
           mc.getProfiler().pop();
           info.cancel();
        }
        if(Thunderhack.moduleManager.get(Aura.class).isEnabled() && Aura.target != null && Aura.mode.getValue() != Aura.Mode.None){
            mc.getProfiler().pop();
            info.cancel();
            mc.crosshairTarget = new EntityHitResult(Aura.target);
        }
    }

    @Inject(method = "loadPrograms", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    void loadAllTheShaders(ResourceFactory factory, CallbackInfo ci, List<ShaderStage> stages, List<Pair<ShaderProgram, Consumer<ShaderProgram>>> shadersToLoad) {
        GlProgram.forEachProgram(loader -> shadersToLoad.add(new Pair<>(loader.getLeft().apply(factory), loader.getRight())));
    }
}