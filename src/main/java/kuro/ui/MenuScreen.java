package kuro.ui;

import static org.lwjgl.nanovg.NanoVG.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import kuro.Config;
import kuro.Kuro;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Siyah, modern, animasyonlu NanoVG menusu (CS2 hile menusu tarzi). */
public final class MenuScreen extends Screen {
   // ---------------------------------------------------------------- yerlesim
   private static final float W = 720.0F;
   private static final float H = 470.0F;
   private static final float SIDE = 176.0F;
   private static final float HEAD = 74.0F;
   private static final float PAD = 18.0F;
   private static final float CARD_H = 54.0F;
   private static final float SLIDER_H = 38.0F;

   private static final String[] TABS = new String[]{"Savaş", "Hareket", "Görsel", "Kamera", "Diğer", "Ayarlar"};
   private static final String[] TAB_SUB = new String[]{
      "Savaş, macro ve legit ayarları",
      "Uçuş, hız ve hareket yardımcıları",
      "ESP ve görsel yardımcılar",
      "Freecam ve Freelook",
      "Yardımcı araçlar",
      "Tema, HUD ve görünüm"
   };
   private static final int[][] TAB_MODS = new int[][]{
      {Kuro.TOTEM, Kuro.CRYSTAL, Kuro.CMACRO, Kuro.AMACRO, Kuro.KILLAURA, Kuro.TRIGGERBOT, Kuro.VELOCITY},
      {Kuro.FLY, Kuro.SPEED, Kuro.SPRINT, Kuro.NOFALL},
      {Kuro.STORAGE, Kuro.BLOCK, Kuro.PLAYER, Kuro.FULLBRIGHT},
      {Kuro.FREECAM, Kuro.FREELOOK},
      {Kuro.BASES, Kuro.FASTPLACE},
      {}
   };
   private static final int SETTINGS_TAB = 5;
   private static final String[] SETTING_NAMES = new String[]{"Filigran", "Modül listesi", "Kısayol paneli", "Bilgi çubuğu", "Bildirimler"};
   private static final String[] SETTING_DESCS = new String[]{
      "Sol üstte kuro / oyuncu / fps / ping / saat",
      "Açık modülleri sağ üstte listeler",
      "Açık modüllerin tuşlarını sol tarafta gösterir",
      "Sağ altta koordinat, yön ve hız",
      "Modül açıp kapatınca sağ altta bildirim çıkar"
   };

   // ---------------------------------------------------------------- ayar sürgüleri
   private record Slider(String name, float min, float max, float step, DoubleSupplier get, DoubleConsumer set) {
   }

   private static final Slider[][] SLIDERS = new Slider[Kuro.NAMES.length][];

   static {
      SLIDERS[Kuro.FLY] = new Slider[]{
         new Slider("Yatay hız", 0.1F, 2.0F, 0.05F, () -> Config.flyH, v -> Config.flyH = (float)v),
         new Slider("Dikey hız", 0.1F, 1.5F, 0.05F, () -> Config.flyV, v -> Config.flyV = (float)v)
      };
      SLIDERS[Kuro.TOTEM] = new Slider[]{
         new Slider("Min gecikme (tick)", 0.0F, 20.0F, 1.0F, () -> Config.totemMin, v -> Config.totemMin = (float)v),
         new Slider("Max gecikme (tick)", 0.0F, 20.0F, 1.0F, () -> Config.totemMax, v -> Config.totemMax = (float)v),
         new Slider("Can eşiği (20 = hep)", 1.0F, 20.0F, 1.0F, () -> Config.totemHealth, v -> Config.totemHealth = (float)v),
         new Slider("Legit mod (0 kapalı / 1 açık)", 0.0F, 1.0F, 1.0F, () -> Config.totemLegit, v -> Config.totemLegit = (float)v)
      };
      SLIDERS[Kuro.KILLAURA] = new Slider[]{
         new Slider("Menzil", 2.5F, 6.0F, 0.1F, () -> Config.auraRange, v -> Config.auraRange = (float)v),
         new Slider("Dönüş hızı (0 = dönme)", 0.0F, 180.0F, 5.0F, () -> Config.auraTurn, v -> Config.auraTurn = (float)v)
      };
      SLIDERS[Kuro.SPEED] = new Slider[]{
         new Slider("Hız", 0.1F, 1.0F, 0.05F, () -> Config.speedValue, v -> Config.speedValue = (float)v)
      };
      SLIDERS[Kuro.VELOCITY] = new Slider[]{
         new Slider("Kalan itme (%)", 0.0F, 100.0F, 5.0F, () -> Config.velocityKeep, v -> Config.velocityKeep = (float)v)
      };
      SLIDERS[Kuro.CMACRO] = new Slider[]{
         new Slider("Koyma gecikmesi (tick)", 0.0F, 6.0F, 1.0F, () -> Config.crystalPlace, v -> Config.crystalPlace = (float)v),
         new Slider("Kırma gecikmesi (tick)", 0.0F, 6.0F, 1.0F, () -> Config.crystalBreak, v -> Config.crystalBreak = (float)v)
      };
      SLIDERS[Kuro.AMACRO] = new Slider[]{
         new Slider("Adım gecikmesi (tick)", 1.0F, 8.0F, 1.0F, () -> Config.anchorDelay, v -> Config.anchorDelay = (float)v)
      };
      SLIDERS[Kuro.FREECAM] = new Slider[]{
         new Slider("Kamera hızı", 0.1F, 3.0F, 0.05F, () -> Config.freecamSpeed, v -> Config.freecamSpeed = (float)v)
      };
   }

   // ---------------------------------------------------------------- tıklama alanları
   private static final int T_TAB = 0;
   private static final int T_CARD = 1;
   private static final int T_CHIP = 2;
   private static final int T_ARROW = 3;
   private static final int T_SLIDER = 4;
   private static final int T_ACTION = 5;
   private static final int T_SWATCH = 6;
   private static final int T_SETTING = 7;

   private record Hit(int type, int a, int b, float x, float y, float w, float h) {
      boolean contains(float px, float py) {
         return px >= x && px <= x + w && py >= y && py <= y + h;
      }
   }

   private final List<Hit> hits = new ArrayList<>();
   private Hit drag;

   // ---------------------------------------------------------------- durum
   private int tab = 0;
   private int listening = -1;
   private float prog = 0.0F;
   private boolean closing;
   private long last = System.nanoTime();
   private float uiScale = 1.0F;
   private float winX;
   private float winY;
   private float time;

   private final Anim tabInd = new Anim(0.0F);
   private final Anim tabFade = new Anim(0.0F);
   private final Anim[] tabHover = new Anim[TABS.length];
   private final Anim[] modHover = new Anim[Kuro.NAMES.length];
   private final Anim[] modOn = new Anim[Kuro.NAMES.length];
   private final Anim[] modExp = new Anim[Kuro.NAMES.length];
   private final Anim[] setOn = new Anim[SETTING_NAMES.length];
   private final Anim[] setHover = new Anim[SETTING_NAMES.length];
   private final Anim scroll = new Anim(0.0F);
   private float scrollTarget;
   private float contentH;

   public MenuScreen() {
      super(Text.literal("Kuro"));
      for (int i = 0; i < TABS.length; i++) {
         tabHover[i] = new Anim(0.0F);
      }

      for (int i = 0; i < Kuro.NAMES.length; i++) {
         modHover[i] = new Anim(0.0F);
         modOn[i] = new Anim(Kuro.on[i] ? 1.0F : 0.0F);
         modExp[i] = new Anim(0.0F);
      }

      for (int i = 0; i < SETTING_NAMES.length; i++) {
         setOn[i] = new Anim(getSetting(i) ? 1.0F : 0.0F);
         setHover[i] = new Anim(0.0F);
      }
   }

   // ---------------------------------------------------------------- Screen

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      // vanilla arka plan / bulanıklık yok; NanoVG kendi karartmasını çizer
   }

   @Override
   public void close() {
      closing = true;
      listening = -1;
      drag = null;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (!Nvg.ensure()) {
         context.drawCenteredTextWithShadow(this.textRenderer, "NanoVG yüklenemedi (log dosyasına bak). Esc ile kapat.", this.width / 2, this.height / 2, 0xFFFFFF);
         if (closing) {
            Kuro.mc.setScreen(null);
         }

         return;
      }

      context.draw();

      long now = System.nanoTime();
      float dt = Math.min(0.1F, (now - last) / 1.0E9F);
      last = now;
      time += dt;

      prog = Anim.clamp(prog + (closing ? -dt : dt) / 0.24F, 0.0F, 1.0F);

      Window win = Kuro.mc.getWindow();
      int fw = win.getFramebufferWidth();
      int fh = win.getFramebufferHeight();
      if (fw <= 0 || fh <= 0) {
         return;
      }

      // pencere her zaman sığsın
      uiScale = Math.min(Theme.scale(fh), Math.min(fh / (H + 24.0F), fw / (W + 24.0F)));
      float lw = fw / uiScale;
      float lh = fh / uiScale;
      winX = (lw - W) * 0.5F;
      winY = (lh - H) * 0.5F;
      Theme.update(dt);

      Nvg.begin(lw, lh, uiScale);
      try {
         draw(lw, lh, dt);
      } finally {
         Nvg.end();
      }

      if (closing && prog <= 0.0F) {
         Kuro.mc.setScreen(null);
      }
   }

   // ---------------------------------------------------------------- girdi

   private float localX() {
      Window w = Kuro.mc.getWindow();
      double sx = (double)w.getFramebufferWidth() / (double)w.getWidth();
      return (float)(Kuro.mc.mouse.getX() * sx / uiScale) - winX;
   }

   private float localY() {
      Window w = Kuro.mc.getWindow();
      double sy = (double)w.getFramebufferHeight() / (double)w.getHeight();
      return (float)(Kuro.mc.mouse.getY() * sy / uiScale) - winY;
   }

   private Hit pick(float x, float y) {
      for (int i = hits.size() - 1; i >= 0; i--) {
         Hit h = hits.get(i);
         if (!h.contains(x, y)) {
            continue;
         }

         if (h.type != T_TAB && (y < HEAD || y > H || x < SIDE || x > W)) {
            continue; // kaydırılan içerik görünür alanın dışında
         }

         return h;
      }

      return null;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (closing) {
         return true;
      }

      listening = -1;

      float x = localX();
      float y = localY();
      Hit h = pick(x, y);
      if (h == null) {
         return true;
      }

      switch (h.type) {
         case T_TAB -> setTab(h.a);
         case T_CARD -> {
            if (button == 1 && SLIDERS[h.a] != null) {
               modExp[h.a].to(modExp[h.a].t > 0.5F ? 0.0F : 1.0F);
            } else if (button == 0) {
               Kuro.toggle(h.a);
            }
         }
         case T_ACTION -> Kuro.toggle(h.a);
         case T_ARROW -> modExp[h.a].to(modExp[h.a].t > 0.5F ? 0.0F : 1.0F);
         case T_CHIP -> {
            if (button == 1) {
               Kuro.binds[h.a] = -1;
               Kuro.saveBinds();
            } else {
               listening = h.a;
            }
         }
         case T_SLIDER -> {
            drag = h;
            applySlider(h, x);
         }
         case T_SWATCH -> {
            Config.accent = h.a;
            Config.save();
         }
         case T_SETTING -> {
            setSetting(h.a, !getSetting(h.a));
            Config.save();
         }
         default -> {
         }
      }

      return true;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (drag != null) {
         Config.save();
         drag = null;
      }

      return true;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (drag != null) {
         applySlider(drag, localX());
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float max = Math.max(0.0F, contentH - (H - HEAD - PAD));
      scrollTarget = Anim.clamp(scrollTarget - (float)verticalAmount * 44.0F, 0.0F, max);
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (listening >= 0) {
         if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            listening = -1;
         } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            Kuro.binds[listening] = -1;
            Kuro.saveBinds();
            listening = -1;
         } else {
            Kuro.binds[listening] = keyCode;
            Kuro.saveBinds();
            listening = -1;
         }

         return true;
      }

      if (Kuro.menuKey != null && Kuro.menuKey.matchesKey(keyCode, scanCode)) {
         close();
         return true;
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void setTab(int t) {
      if (t == tab) {
         return;
      }

      tab = t;
      scrollTarget = 0.0F;
      scroll.snap(0.0F);
      tabFade.snap(0.0F);
   }

   private void applySlider(Hit h, float mx) {
      Slider s = SLIDERS[h.a][h.b];
      float t = Anim.clamp((mx - h.x) / h.w, 0.0F, 1.0F);
      float v = s.min + t * (s.max - s.min);
      v = Math.round(v / s.step) * s.step;
      s.set.accept(Anim.clamp(v, s.min, s.max));
   }

   private static boolean getSetting(int k) {
      return switch (k) {
         case 0 -> Config.watermark;
         case 1 -> Config.moduleList;
         case 2 -> Config.keybinds;
         case 3 -> Config.info;
         default -> Config.toasts;
      };
   }

   private static void setSetting(int k, boolean v) {
      switch (k) {
         case 0 -> Config.watermark = v;
         case 1 -> Config.moduleList = v;
         case 2 -> Config.keybinds = v;
         case 3 -> Config.info = v;
         default -> Config.toasts = v;
      }
   }

   // ---------------------------------------------------------------- çizim

   private void draw(float lw, float lh, float dt) {
      hits.clear();
      float e = Anim.easeOutCubic(prog);
      int accent = Theme.accent();
      int accent2 = Theme.accent2();

      // arka plan karartması
      Nvg.rect(0.0F, 0.0F, lw, lh, 0.0F, Theme.alpha(0x000000, 0.52F * e));

      nvgSave(Nvg.ctx);
      nvgTranslate(Nvg.ctx, winX + W * 0.5F, winY + H * 0.5F + (1.0F - e) * 14.0F);
      float sc = 0.955F + 0.045F * e;
      nvgScale(Nvg.ctx, sc, sc);
      nvgTranslate(Nvg.ctx, -W * 0.5F, -H * 0.5F);
      nvgGlobalAlpha(Nvg.ctx, e);

      // pencere
      Nvg.glow(0.0F, 8.0F, W, H, 12.0F, 46.0F, 0xB0000000);
      Nvg.rect(0.0F, 0.0F, W, H, 12.0F, Theme.BG);
      Nvg.rectVar(0.0F, 0.0F, SIDE, H, 12.0F, 0.0F, 0.0F, 12.0F, Theme.BG_SIDE);
      Nvg.line(SIDE, 0.0F, SIDE, H, 1.0F, 0x14FFFFFF);
      // ince vurgu çizgisi (üst kenar)
      Nvg.rectGradH(14.0F, 0.0F, W - 28.0F, 2.0F, 1.0F, Theme.a(accent, 0.0F), accent);
      Nvg.rectGradH(W * 0.5F, 0.0F, W * 0.5F - 14.0F, 2.0F, 1.0F, accent, Theme.a(accent2, 0.0F));
      Nvg.border(0.0F, 0.0F, W, H, 12.0F, 1.0F, 0x1AFFFFFF);

      drawSidebar(dt);
      drawHeader();
      drawContent(dt);

      nvgRestore(Nvg.ctx);
   }

   private void drawSidebar(float dt) {
      int accent = Theme.accent();
      float mx = localX();
      float my = localY();

      // logo
      Nvg.textL("kuro", 22.0F, 36.0F, 26.0F, true, accent);
      Nvg.textL("v1.3.0  ·  1.21.4", 23.0F, 56.0F, 10.5F, false, Theme.TEXT_MUTED);

      // aktif sekme göstergesi (kayar)
      float y0 = 86.0F;
      float rowH = 40.0F;
      tabInd.to(tab).tick(dt, 14.0F);
      float iy = y0 + tabInd.v * rowH;
      Nvg.rect(12.0F, iy, SIDE - 24.0F, 34.0F, 8.0F, Theme.a(accent, 0.13F));
      Nvg.border(12.0F, iy, SIDE - 24.0F, 34.0F, 8.0F, 1.0F, Theme.a(accent, 0.30F));
      Nvg.rect(12.0F, iy + 8.0F, 3.0F, 18.0F, 1.5F, accent);

      for (int i = 0; i < TABS.length; i++) {
         float ry = y0 + i * rowH;
         boolean hov = mx >= 12.0F && mx <= SIDE - 12.0F && my >= ry && my <= ry + 34.0F;
         tabHover[i].to(hov ? 1.0F : 0.0F).tick(dt, 16.0F);
         float sel = Anim.clamp(1.0F - Math.abs(tabInd.v - i), 0.0F, 1.0F);
         int txt = Theme.mix(Theme.mix(Theme.TEXT_DIM, 0xFFCFCFD8, tabHover[i].v), Theme.TEXT, sel);
         int ic = Theme.mix(txt, accent, sel);
         icon(i, 34.0F, ry + 17.0F, ic);
         Nvg.textL(TABS[i], 54.0F, ry + 17.5F, 13.5F, sel > 0.5F, txt);
         hits.add(new Hit(T_TAB, i, 0, 12.0F, ry, SIDE - 24.0F, 34.0F));
      }

      // alt: oyuncu + menü tuşu
      float by = H - 56.0F;
      Nvg.line(16.0F, by - 8.0F, SIDE - 16.0F, by - 8.0F, 1.0F, 0x12FFFFFF);
      String user = Kuro.mc.getSession().getUsername();
      Nvg.circle(32.0F, by + 18.0F, 13.0F, Theme.a(accent, 0.2F));
      Nvg.circleStroke(32.0F, by + 18.0F, 13.0F, 1.0F, Theme.a(accent, 0.6F));
      Nvg.textC(user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase(), 32.0F, by + 18.5F, 12.5F, true, accent);
      Nvg.textL(user, 52.0F, by + 12.0F, 12.5F, true, Theme.TEXT);
      String mk = Kuro.menuKey == null ? "-" : Keys.name(menuKeyCode());
      Nvg.textL("Menü  [" + mk + "]", 52.0F, by + 27.0F, 10.5F, false, Theme.TEXT_MUTED);
   }

   private static int menuKeyCode() {
      String key = Kuro.menuKey.getBoundKeyTranslationKey();
      return net.minecraft.client.util.InputUtil.fromTranslationKey(key).getCode();
   }

   private void drawHeader() {
      Nvg.textL(TABS[tab], SIDE + PAD, 30.0F, 21.0F, true, Theme.TEXT);
      Nvg.textL(TAB_SUB[tab], SIDE + PAD, 53.0F, 11.5F, false, Theme.TEXT_DIM);
      Nvg.textR("Sağ tık: ayar aç  ·  Esc: kapat", W - PAD, 30.0F, 10.5F, false, Theme.TEXT_MUTED);
      Nvg.line(SIDE + PAD, HEAD - 2.0F, W - PAD, HEAD - 2.0F, 1.0F, 0x12FFFFFF);
   }

   private void drawContent(float dt) {
      float ca = Anim.easeOutCubic(prog);
      tabFade.to(1.0F).tick(dt, 14.0F);
      float fade = Anim.easeOutCubic(tabFade.v);

      float viewH = H - HEAD - PAD;
      float max = Math.max(0.0F, contentH - viewH);
      scrollTarget = Anim.clamp(scrollTarget, 0.0F, max);
      scroll.to(scrollTarget).tick(dt, 18.0F);

      nvgSave(Nvg.ctx);
      nvgIntersectScissor(Nvg.ctx, SIDE, HEAD, W - SIDE, H - HEAD - 4.0F);
      nvgGlobalAlpha(Nvg.ctx, ca * fade);

      float x = SIDE + PAD;
      float w = W - SIDE - PAD * 2.0F;
      float y = HEAD + 8.0F - scroll.v + (1.0F - fade) * 10.0F;
      float startY = y;

      if (tab == SETTINGS_TAB) {
         y = drawSettings(x, y, w, dt, ca * fade);
      } else {
         for (int id : TAB_MODS[tab]) {
            y += drawModuleCard(id, x, y, w, dt, ca * fade) + 10.0F;
         }
      }

      contentH = y - startY + 8.0F;

      // kaydırma çubuğu
      if (max > 0.0F) {
         float trackH = H - HEAD - 16.0F;
         float thumbH = Math.max(28.0F, trackH * viewH / contentH);
         float ty = HEAD + 8.0F + (trackH - thumbH) * (scroll.v / max);
         Nvg.rect(W - 8.0F, ty, 3.0F, thumbH, 1.5F, 0x30FFFFFF);
      }

      nvgRestore(Nvg.ctx);
   }

   // ---------------------------------------------------------------- modül kartı

   private float drawModuleCard(int id, float x, float y, float w, float dt, float alpha) {
      float mx = localX();
      float my = localY();
      boolean action = id == Kuro.BASES;
      Slider[] sl = SLIDERS[id];
      int accent = Theme.accent();

      modOn[id].to(Kuro.on[id] ? 1.0F : 0.0F).tick(dt, 14.0F);
      modExp[id].tick(dt, 14.0F);
      float on = action ? 0.0F : modOn[id].v;
      float exp = modExp[id].v;
      float extra = sl == null ? 0.0F : exp * (sl.length * SLIDER_H + 6.0F);
      float total = CARD_H + extra;

      boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + CARD_H && my >= HEAD && my <= H;
      modHover[id].to(hov ? 1.0F : 0.0F).tick(dt, 16.0F);
      float hv = modHover[id].v;

      // kart
      Nvg.rect(x, y, w, total, 9.0F, Theme.alpha(0xFFFFFF, 0.035F + 0.028F * hv));
      Nvg.border(x, y, w, total, 9.0F, 1.0F, Theme.mix(0x0DFFFFFF, Theme.a(accent, 0.38F), on));
      if (on > 0.01F) {
         float bh = 26.0F * on;
         Nvg.glow(x, y + (CARD_H - bh) * 0.5F, 3.0F, bh, 1.5F, 10.0F, Theme.a(accent, 0.45F * on));
         Nvg.rect(x, y + (CARD_H - bh) * 0.5F, 3.0F, bh, 1.5F, accent);
      }

      hits.add(new Hit(T_CARD, id, 0, x, y, w, CARD_H));

      // sağdan sola: anahtar / buton, tuş rozeti, ok
      float rx = x + w - 16.0F;
      if (action) {
         float bw = 92.0F;
         float bx = rx - bw;
         Nvg.rectGradH(bx, y + 14.0F, bw, 26.0F, 6.0F, accent, Theme.accent2());
         Nvg.textC("Çalıştır", bx + bw * 0.5F, y + 27.5F, 12.0F, true, 0xFF0B0B0F);
         hits.add(new Hit(T_ACTION, id, 0, bx, y + 14.0F, bw, 26.0F));
         rx = bx - 10.0F;
      } else {
         switchControl(rx - 34.0F, y + (CARD_H - 18.0F) * 0.5F, on);
         rx -= 34.0F + 12.0F;
      }

      // tuş rozeti
      boolean lis = listening == id;
      String label = lis ? "..." : Keys.name(Kuro.binds[id]);
      float cw = Math.max(36.0F, Nvg.textWidth(label, 11.5F, true) + 18.0F);
      float cx = rx - cw;
      float cy = y + (CARD_H - 22.0F) * 0.5F;
      float pulse = 0.5F + 0.5F * (float)Math.sin(time * 7.0F);
      Nvg.rect(cx, cy, cw, 22.0F, 6.0F, lis ? Theme.a(accent, 0.16F) : 0x14FFFFFF);
      Nvg.border(cx, cy, cw, 22.0F, 6.0F, 1.0F, lis ? Theme.a(accent, 0.45F + 0.4F * pulse) : 0x14FFFFFF);
      Nvg.textC(label, cx + cw * 0.5F, cy + 11.5F, 11.5F, true, lis ? accent : Theme.TEXT_DIM);
      hits.add(new Hit(T_CHIP, id, 0, cx, cy, cw, 22.0F));
      rx = cx - 10.0F;

      // genişletme oku
      if (sl != null) {
         float ax = rx - 20.0F;
         float ay = y + CARD_H * 0.5F;
         nvgSave(Nvg.ctx);
         nvgTranslate(Nvg.ctx, ax + 10.0F, ay);
         nvgRotate(Nvg.ctx, (float)Math.toRadians(-90.0F + 90.0F * exp));
         nvgBeginPath(Nvg.ctx);
         nvgMoveTo(Nvg.ctx, -3.5F, -2.0F);
         nvgLineTo(Nvg.ctx, 0.0F, 2.0F);
         nvgLineTo(Nvg.ctx, 3.5F, -2.0F);
         nvgLineCap(Nvg.ctx, NVG_ROUND);
         nvgLineJoin(Nvg.ctx, NVG_ROUND);
         Nvg.stroke(1.6F, Theme.mix(Theme.TEXT_MUTED, Theme.TEXT, Math.max(hv, exp)));
         nvgRestore(Nvg.ctx);
         hits.add(new Hit(T_ARROW, id, 0, ax - 4.0F, y + 8.0F, 28.0F, CARD_H - 16.0F));
         rx = ax - 6.0F;
      }

      // ad + açıklama (sağdaki kontrollerin soluna sığdır)
      nvgSave(Nvg.ctx);
      nvgIntersectScissor(Nvg.ctx, x + 14.0F, y, Math.max(0.0F, rx - x - 18.0F), CARD_H);
      Nvg.textL(Kuro.NAMES[id], x + 18.0F, y + 21.0F, 14.0F, true, Theme.mix(0xFFB4B4C0, Theme.TEXT, Math.max(on, hv * 0.6F)));
      Nvg.textL(Kuro.DESCS[id], x + 18.0F, y + 38.5F, 11.0F, false, Theme.TEXT_MUTED);
      nvgRestore(Nvg.ctx);

      // sürgüler
      if (sl != null && exp > 0.01F) {
         nvgSave(Nvg.ctx);
         nvgIntersectScissor(Nvg.ctx, x, y + CARD_H, w, extra);
         nvgGlobalAlpha(Nvg.ctx, alpha * exp);
         Nvg.line(x + 16.0F, y + CARD_H - 1.0F, x + w - 16.0F, y + CARD_H - 1.0F, 1.0F, 0x10FFFFFF);
         for (int k = 0; k < sl.length; k++) {
            sliderRow(id, k, sl[k], x, y + CARD_H + k * SLIDER_H, w);
         }

         nvgRestore(Nvg.ctx);
      }

      return total;
   }

   private void sliderRow(int id, int k, Slider s, float x, float y, float w) {
      int accent = Theme.accent();
      float v = (float)s.get.getAsDouble();
      float t = Anim.clamp((v - s.min) / (s.max - s.min), 0.0F, 1.0F);
      float tx = x + 18.0F;
      float tw = w - 36.0F;
      float ty = y + 26.0F;
      boolean dragging = drag != null && drag.type == T_SLIDER && drag.a == id && drag.b == k;
      boolean hov = dragging || (localX() >= tx - 6.0F && localX() <= tx + tw + 6.0F && localY() >= ty - 9.0F && localY() <= ty + 9.0F);

      Nvg.textL(s.name, tx, y + 12.0F, 12.0F, false, Theme.TEXT_DIM);
      Nvg.textR(s.step >= 1.0F ? Integer.toString(Math.round(v)) : Float.toString(Math.round(v * 100.0F) / 100.0F), tx + tw, y + 12.0F, 12.0F, true, Theme.TEXT);
      Nvg.rect(tx, ty - 2.0F, tw, 4.0F, 2.0F, 0x1AFFFFFF);
      if (t > 0.0F) {
         Nvg.rectGradH(tx, ty - 2.0F, Math.max(4.0F, tw * t), 4.0F, 2.0F, accent, Theme.accent2());
      }

      float kx = tx + tw * t;
      if (hov) {
         Nvg.glow(kx - 6.0F, ty - 6.0F, 12.0F, 12.0F, 6.0F, 10.0F, Theme.a(accent, 0.55F));
      }

      Nvg.circle(kx, ty, hov ? 6.5F : 5.5F, 0xFFFFFFFF);
      hits.add(new Hit(T_SLIDER, id, k, tx, ty - 10.0F, tw, 20.0F));
   }

   // ---------------------------------------------------------------- ayarlar sekmesi

   private float drawSettings(float x, float y, float w, float dt, float alpha) {
      float mx = localX();
      float my = localY();
      int accent = Theme.accent();

      Nvg.textL("VURGU RENGİ", x + 2.0F, y + 8.0F, 10.5F, true, Theme.TEXT_MUTED);
      y += 22.0F;

      float ch = 64.0F;
      Nvg.rect(x, y, w, ch, 9.0F, 0x09FFFFFF);
      Nvg.border(x, y, w, ch, 9.0F, 1.0F, 0x0DFFFFFF);
      for (int i = 0; i < Theme.ACCENTS.length; i++) {
         float cx = x + 30.0F + i * 40.0F;
         float cy = y + ch * 0.5F;
         int c = 0xFF000000 | Theme.ACCENTS[i];
         boolean hov = Math.hypot(mx - cx, my - cy) <= 14.0F && my >= HEAD;
         boolean sel = Config.accent == i;
         if (sel) {
            Nvg.glow(cx - 11.0F, cy - 11.0F, 22.0F, 22.0F, 11.0F, 12.0F, Theme.a(c, 0.5F));
            Nvg.circleStroke(cx, cy, 16.0F, 2.0F, c);
         }

         Nvg.circle(cx, cy, hov || sel ? 11.5F : 10.5F, c);
         hits.add(new Hit(T_SWATCH, i, 0, cx - 15.0F, cy - 15.0F, 30.0F, 30.0F));
      }

      Nvg.textR(Theme.ACCENT_NAMES[Config.accent], x + w - 16.0F, y + ch * 0.5F + 0.5F, 12.5F, true, accent);
      y += ch + 18.0F;

      Nvg.textL("HUD", x + 2.0F, y + 8.0F, 10.5F, true, Theme.TEXT_MUTED);
      y += 22.0F;

      for (int i = 0; i < SETTING_NAMES.length; i++) {
         float rh = 50.0F;
         boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + rh && my >= HEAD;
         setHover[i].to(hov ? 1.0F : 0.0F).tick(dt, 16.0F);
         setOn[i].to(getSetting(i) ? 1.0F : 0.0F).tick(dt, 14.0F);

         Nvg.rect(x, y, w, rh, 9.0F, Theme.alpha(0xFFFFFF, 0.035F + 0.028F * setHover[i].v));
         Nvg.border(x, y, w, rh, 9.0F, 1.0F, Theme.mix(0x0DFFFFFF, Theme.a(accent, 0.38F), setOn[i].v));
         Nvg.textL(SETTING_NAMES[i], x + 18.0F, y + 19.0F, 13.5F, true, Theme.mix(0xFFB4B4C0, Theme.TEXT, Math.max(setOn[i].v, setHover[i].v * 0.6F)));
         Nvg.textL(SETTING_DESCS[i], x + 18.0F, y + 35.5F, 11.0F, false, Theme.TEXT_MUTED);
         switchControl(x + w - 16.0F - 34.0F, y + (rh - 18.0F) * 0.5F, setOn[i].v);
         hits.add(new Hit(T_SETTING, i, 0, x, y, w, rh));
         y += rh + 8.0F;
      }

      return y;
   }

   // ---------------------------------------------------------------- bileşenler

   private static void switchControl(float x, float y, float t) {
      int accent = Theme.accent();
      float e = Anim.easeOutCubic(t);
      int track = Theme.mix(0xFF26262F, accent, e);
      if (e > 0.01F) {
         Nvg.glow(x, y, 34.0F, 18.0F, 9.0F, 10.0F, Theme.a(accent, 0.35F * e));
      }

      Nvg.rect(x, y, 34.0F, 18.0F, 9.0F, track);
      float kx = x + 9.0F + 16.0F * t;
      Nvg.circle(kx, y + 9.0F, 6.5F, Theme.mix(0xFFB9B9C6, 0xFFFFFFFF, e));
   }

   /** Sekme simgeleri (vektör). */
   private static void icon(int i, float cx, float cy, int color) {
      long c = Nvg.ctx;
      nvgSave(c);
      nvgTranslate(c, cx, cy);
      nvgLineCap(c, NVG_ROUND);
      nvgLineJoin(c, NVG_ROUND);
      nvgBeginPath(c);
      switch (i) {
         case 0 -> { // nişangah
            nvgCircle(c, 0.0F, 0.0F, 5.5F);
            nvgMoveTo(c, 0.0F, -9.0F);
            nvgLineTo(c, 0.0F, -3.5F);
            nvgMoveTo(c, 0.0F, 9.0F);
            nvgLineTo(c, 0.0F, 3.5F);
            nvgMoveTo(c, -9.0F, 0.0F);
            nvgLineTo(c, -3.5F, 0.0F);
            nvgMoveTo(c, 9.0F, 0.0F);
            nvgLineTo(c, 3.5F, 0.0F);
            Nvg.stroke(1.6F, color);
         }
         case 1 -> { // ok
            nvgMoveTo(c, -7.0F, 5.5F);
            nvgLineTo(c, 8.0F, 0.0F);
            nvgLineTo(c, -7.0F, -5.5F);
            nvgLineTo(c, -4.0F, 0.0F);
            nvgClosePath(c);
            Nvg.stroke(1.6F, color);
         }
         case 2 -> { // göz
            nvgMoveTo(c, -9.0F, 0.0F);
            nvgBezierTo(c, -4.5F, -7.0F, 4.5F, -7.0F, 9.0F, 0.0F);
            nvgBezierTo(c, 4.5F, 7.0F, -4.5F, 7.0F, -9.0F, 0.0F);
            Nvg.stroke(1.6F, color);
            nvgBeginPath(c);
            nvgCircle(c, 0.0F, 0.0F, 2.6F);
            Nvg.stroke(1.6F, color);
         }
         case 3 -> { // kamera
            nvgRoundedRect(c, -8.5F, -4.5F, 17.0F, 11.5F, 2.5F);
            nvgMoveTo(c, -3.0F, -4.5F);
            nvgLineTo(c, -1.8F, -7.0F);
            nvgLineTo(c, 1.8F, -7.0F);
            nvgLineTo(c, 3.0F, -4.5F);
            Nvg.stroke(1.6F, color);
            nvgBeginPath(c);
            nvgCircle(c, 0.0F, 1.2F, 3.0F);
            Nvg.stroke(1.6F, color);
         }
         case 4 -> { // ızgara
            nvgRoundedRect(c, -7.5F, -7.5F, 6.0F, 6.0F, 1.6F);
            nvgRoundedRect(c, 1.5F, -7.5F, 6.0F, 6.0F, 1.6F);
            nvgRoundedRect(c, -7.5F, 1.5F, 6.0F, 6.0F, 1.6F);
            nvgRoundedRect(c, 1.5F, 1.5F, 6.0F, 6.0F, 1.6F);
            Nvg.stroke(1.6F, color);
         }
         default -> { // sürgüler
            nvgMoveTo(c, -8.0F, -5.0F);
            nvgLineTo(c, 8.0F, -5.0F);
            nvgMoveTo(c, -8.0F, 0.0F);
            nvgLineTo(c, 8.0F, 0.0F);
            nvgMoveTo(c, -8.0F, 5.0F);
            nvgLineTo(c, 8.0F, 5.0F);
            Nvg.stroke(1.6F, color);
            Nvg.circle(-3.0F, -5.0F, 2.4F, 0xFF0B0B0F);
            Nvg.circle(4.0F, 0.0F, 2.4F, 0xFF0B0B0F);
            Nvg.circle(-1.0F, 5.0F, 2.4F, 0xFF0B0B0F);
            Nvg.circleStroke(-3.0F, -5.0F, 2.4F, 1.5F, color);
            Nvg.circleStroke(4.0F, 0.0F, 2.4F, 1.5F, color);
            Nvg.circleStroke(-1.0F, 5.0F, 2.4F, 1.5F, color);
         }
      }

      nvgRestore(c);
   }
}
