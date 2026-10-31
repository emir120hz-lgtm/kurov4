package kuro;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class Render {
   static final MinecraftClient mc = MinecraftClient.getInstance();
   static final double MAX_DIST = 256.0;

   public static void render(WorldRenderContext ctx) {
      if (Kuro.on[Kuro.STORAGE] || Kuro.on[Kuro.BLOCK] || Kuro.on[Kuro.PLAYER]) {
         if (mc.world != null && mc.player != null) {
            MatrixStack ms = ctx.matrixStack();
            if (ms != null) {
               Vec3d cam = ctx.camera().getPos();
               float td = ctx.tickCounter().getTickDelta(false);
               ms.push();
               ms.translate(-cam.x, -cam.y, -cam.z);
               Matrix4f m = ms.peek().getPositionMatrix();
               RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
               RenderSystem.disableDepthTest();
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.lineWidth(2.0F);
               boxCount = 0;
               Vec3d camLook = Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw());
               BufferBuilder b = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
               if (Kuro.on[Kuro.STORAGE]) {
                  drawHits(b, m, Mods.storageHits.values(), cam, camLook, 65536.0);
               }

               if (Kuro.on[Kuro.BLOCK]) {
                  drawHits(b, m, Mods.blockHits.values(), cam, camLook, 16384.0);
               }

               if (Kuro.on[Kuro.PLAYER]) {
                  Vec3d start = cam.add(camLook);

                  for (AbstractClientPlayerEntity e : mc.world.getPlayers()) {
                     if (e != mc.player) {
                        Vec3d off = e.getLerpedPos(td).subtract(e.getPos());
                        Box bb = e.getBoundingBox().offset(off);
                        box(b, m, bb, 1.0F, 0.2F, 0.2F, 1.0F);
                        Vec3d c = bb.getCenter();
                        line(b, m, start.x, start.y, start.z, c.x, c.y, c.z, 1.0F, 0.2F, 0.2F, 0.7F);
                     }
                  }
               }

               BuiltBuffer built = b.endNullable();
               if (built != null) {
                  BufferRenderer.drawWithGlobalProgram(built);
               }

               RenderSystem.enableDepthTest();
               RenderSystem.disableBlend();
               ms.pop();
            }
         }
      }
   }

   // Kare basina cizilecek en fazla kutu sayisi (aksi halde binlerce kutu FPS'i dusurur)
   static final int MAX_BOXES = 2500;
   static int boxCount;

   static void drawHits(BufferBuilder b, Matrix4f m, Iterable<List<Mods.Hit>> all, Vec3d cam, Vec3d look, double maxDistSq) {
      for (List<Mods.Hit> l : all) {
         for (int i = 0, n = l.size(); i < n; i++) {
            if (boxCount >= MAX_BOXES) {
               return;
            }

            Mods.Hit h = l.get(i);
            BlockPos p = h.pos();
            double dx = (double)p.getX() + 0.5 - cam.x;
            double dy = (double)p.getY() + 0.5 - cam.y;
            double dz = (double)p.getZ() + 0.5 - cam.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > maxDistSq) {
               continue;
            }

            // kameranin arkasinda kalanlari cizme (yaklasik gorus konisi testi)
            if (d2 > 9.0 && dx * look.x + dy * look.y + dz * look.z < 0.0) {
               continue;
            }

            int c = h.color();
            box(
               b, m,
               (double)p.getX(), (double)p.getY(), (double)p.getZ(),
               (double)(p.getX() + 1), (double)(p.getY() + 1), (double)(p.getZ() + 1),
               (float)(c >> 16 & 0xFF) / 255.0F, (float)(c >> 8 & 0xFF) / 255.0F, (float)(c & 0xFF) / 255.0F, 1.0F
            );
            boxCount++;
         }
      }
   }

   static void box(BufferBuilder b, Matrix4f m, Box bx, float r, float g, float bl, float a) {
      box(b, m, bx.minX, bx.minY, bx.minZ, bx.maxX, bx.maxY, bx.maxZ, r, g, bl, a);
   }

   static void box(BufferBuilder b, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float bl, float a) {
      line(b, m, x1, y1, z1, x2, y1, z1, r, g, bl, a);
      line(b, m, x2, y1, z1, x2, y1, z2, r, g, bl, a);
      line(b, m, x2, y1, z2, x1, y1, z2, r, g, bl, a);
      line(b, m, x1, y1, z2, x1, y1, z1, r, g, bl, a);
      line(b, m, x1, y2, z1, x2, y2, z1, r, g, bl, a);
      line(b, m, x2, y2, z1, x2, y2, z2, r, g, bl, a);
      line(b, m, x2, y2, z2, x1, y2, z2, r, g, bl, a);
      line(b, m, x1, y2, z2, x1, y2, z1, r, g, bl, a);
      line(b, m, x1, y1, z1, x1, y2, z1, r, g, bl, a);
      line(b, m, x2, y1, z1, x2, y2, z1, r, g, bl, a);
      line(b, m, x2, y1, z2, x2, y2, z2, r, g, bl, a);
      line(b, m, x1, y1, z2, x1, y2, z2, r, g, bl, a);
   }

   static void line(BufferBuilder b, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float bl, float a) {
      b.vertex(m, (float)x1, (float)y1, (float)z1).color(r, g, bl, a);
      b.vertex(m, (float)x2, (float)y2, (float)z2).color(r, g, bl, a);
   }
}
