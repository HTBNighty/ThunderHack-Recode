package thunder.hack.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thunder.hack.Thunderhack;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.modules.client.MainSettings;
import thunder.hack.modules.render.Chams;
import thunder.hack.setting.impl.ColorSetting;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
    private LivingEntity lastEntity;

    private float originalYaw;
    private float originalHeadYaw;
    private float originalBodyYaw;
    private float originalPitch;

    private float originalPrevYaw;
    private float originalPrevHeadYaw;
    private float originalPrevBodyYaw;
    private float originalPrevPitch;

    @Inject(method = "render", at = @At("HEAD"))
    public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && MainSettings.renderRotations.getValue()) {
            originalYaw = livingEntity.getYaw();
            originalHeadYaw = livingEntity.headYaw;
            originalBodyYaw = livingEntity.bodyYaw;
            originalPitch = livingEntity.getPitch();
            originalPrevYaw = livingEntity.prevYaw;
            originalPrevHeadYaw = livingEntity.prevHeadYaw;
            originalPrevBodyYaw = livingEntity.prevBodyYaw;
            originalPrevPitch = livingEntity.prevPitch;
            livingEntity.setYaw(((IClientPlayerEntity) MinecraftClient.getInstance().player).getLastYaw());
            livingEntity.headYaw = ((IClientPlayerEntity) MinecraftClient.getInstance().player).getLastYaw();
            livingEntity.bodyYaw = ((IClientPlayerEntity) MinecraftClient.getInstance().player).getLastYaw();
            livingEntity.setPitch(((IClientPlayerEntity) MinecraftClient.getInstance().player).getLastPitch());
            livingEntity.prevYaw = Thunderhack.playerManager.lastYaw;
            livingEntity.prevHeadYaw =  Thunderhack.playerManager.lastYaw;
            livingEntity.prevBodyYaw = Thunderhack.playerManager.lastYaw;
            livingEntity.prevPitch = Thunderhack.playerManager.lastPitch;
        }
        lastEntity = livingEntity;
        if (Thunderhack.moduleManager.get(Chams.class).isEnabled()) {
          //  GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
          //  GL11.glPolygonOffset(1.0f, -1100000.0f);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && MainSettings.renderRotations.getValue()) {
            livingEntity.setYaw(originalYaw);
            livingEntity.headYaw = originalHeadYaw;
            livingEntity.bodyYaw = originalBodyYaw;
            livingEntity.setPitch(originalPitch);
            livingEntity.prevYaw = originalPrevYaw;
            livingEntity.prevHeadYaw = originalPrevHeadYaw;
            livingEntity.prevBodyYaw = originalPrevBodyYaw;
            livingEntity.prevPitch = originalPitch;
        }
        if (Thunderhack.moduleManager.get(Chams.class).isEnabled()) {
         //   GL11.glPolygonOffset(1.0f, 1100000.0f);
         //   GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }


    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"))
    private void onRenderModel(EntityModel entityModel, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        if(Thunderhack.moduleManager.get(Chams.class).isEnabled()){
            ColorSetting clr = Thunderhack.moduleManager.get(Chams.class).getEntityColor(lastEntity);
            entityModel.render(matrices, vertices, light, overlay, clr.getRed() / 255F, clr.getGreen() / 255F, clr.getBlue() / 255F, clr.getAlpha() / 255F);
            return;
        }
      //  RenderSystem.enableDepthTest();
       // RenderSystem.depthFunc(GL30.GL_ALWAYS);
        entityModel.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", ordinal = 0))
    private void onScale(Args args) {
        if(Thunderhack.moduleManager.get(Chams.class).isEnabled()) {
            float scale = Thunderhack.moduleManager.get(Chams.class).getEntityScale(lastEntity);
            args.set(0, -scale);
            args.set(1, -scale);
            args.set(2, scale);
        }
    }
}