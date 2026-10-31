package kuro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import kuro.ui.Hud;
import kuro.ui.MenuScreen;
import kuro.mixin.MinecraftClientAccessor;
import kuro.ui.Toasts;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kuro implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("kuro");
   public static final MinecraftClient mc = MinecraftClient.getInstance();

   // Modul indeksleri (Mods / Render bunlari kullanir)
   public static final int FLY = 0;
   public static final int TOTEM = 1;
   public static final int CRYSTAL = 2;
   public static final int STORAGE = 3;
   public static final int BLOCK = 4;
   public static final int PLAYER = 5;
   public static final int BASES = 6; // aksiyon: acik/kapali durumu yok
   public static final int FREECAM = 7;
   public static final int FREELOOK = 8;
   public static final int CMACRO = 9;
   public static final int AMACRO = 10;
   public static final int SPRINT = 11;
   public static final int NOFALL = 12;
   public static final int SPEED = 13;
   public static final int FULLBRIGHT = 14;
   public static final int KILLAURA = 15;
   public static final int TRIGGERBOT = 16;
   public static final int VELOCITY = 17;
   public static final int FASTPLACE = 18;

   public static final String[] NAMES = new String[]{
      "Fly", "AutoTotem", "AutoCrystal", "StorageESP", "BlockESP", "PlayerESP", "BaseList", "Freecam", "Freelook", "CrystalMacro", "AnchorMacro",
      "Sprint", "NoFall", "Speed", "Fullbright", "KillAura", "TriggerBot", "Velocity", "FastPlace"
   };
   public static final String[] DESCS = new String[]{
      "Havada uçma. WASD, boşluk ve shift ile yön ver",
      "Totemi otomatik olarak sol ele alır",
      "Yakındaki oyunculara otomatik kristal koyar ve patlatır",
      "Sandık, varil, shulker, huni ve fırın gibi depoları çerçeveler",
      "kuro_blocks.txt içindeki blokları çerçeveler",
      "Oyuncuları kutu ve çizgiyle gösterir",
      "Depo yoğunluğuna göre olası base konumlarını listeler",
      "Kamerayı karakterden ayırıp serbestçe uçurur",
      "Karakter yönü sabit kalırken etrafına bakmanı sağlar",
      "Sağ tık basılıyken obsidyene kristal koyar, kristale bakınca kırar",
      "Sağ tık basılıyken anchor'ı glowstone ile doldurup patlatır",
      "İleri giderken otomatik koşar",
      "Düşme hasarını engeller",
      "Yürüme hızını artırır",
      "Karanlıkta bile her yeri aydınlık gösterir",
      "Menzildeki oyuncuya yönelip saldırı bekleme süresi dolunca vurur",
      "Tam nişan aldığın oyuncuya otomatik vurur",
      "Aldığın itmeyi (knockback) engeller veya azaltır",
      "Blok koyma / eşya kullanma gecikmesini kaldırır"
   };
   public static final boolean[] on = new boolean[NAMES.length];
   public static final int[] binds = new int[]{71, 90, 67, 86, 66, 78, 72, 74, 76, 82, 75, -1, -1, -1, -1, -1, -1, -1, -1};
   static final boolean[] prev = new boolean[NAMES.length];
   public static KeyBinding menuKey;

   public void onInitializeClient() {
      Config.load();
      loadBinds();
      menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.kuro.menu", InputUtil.Type.KEYSYM, 344, "Kuro"));

      ClientTickEvents.END_CLIENT_TICK.register(c -> {
         if (c.player == null || c.world == null) {
            // dunyadan cikildi: kamera modlarini kapat
            if (on[FREECAM] || on[FREELOOK]) {
               setOn(FREECAM, false);
               setOn(FREELOOK, false);
            }

            return;
         }

         while (menuKey.wasPressed()) {
            if (c.currentScreen == null) {
               c.setScreen(new MenuScreen());
            }
         }

         long win = c.getWindow().getHandle();
         for (int i = 0; i < binds.length; i++) {
            boolean down = binds[i] >= 0 && InputUtil.isKeyPressed(win, binds[i]);
            if (down && !prev[i] && c.currentScreen == null) {
               toggle(i);
            }

            prev[i] = down;
         }

         Hud.tick();
         Freecam.tick();

         if (on[FLY]) {
            Mods.fly();
         }

         if (on[TOTEM]) {
            Mods.totem();
         }

         if (on[CRYSTAL]) {
            Mods.crystal();
         }

         if (on[CMACRO]) {
            Macros.crystal();
         }

         if (on[AMACRO]) {
            Macros.anchor();
         }

         if (on[SPRINT]) {
            Extras.sprint();
         }

         if (on[NOFALL]) {
            Extras.noFall();
         }

         if (on[SPEED]) {
            Extras.speed();
         }

         if (on[FULLBRIGHT]) {
            Extras.fullbright();
         }

         if (on[KILLAURA]) {
            Extras.killAura();
         }

         if (on[TRIGGERBOT]) {
            Extras.triggerBot();
         }

         if (on[FASTPLACE]) {
            ((MinecraftClientAccessor)mc).kuro$setItemUseCooldown(0);
         }

         if (on[STORAGE] || on[BLOCK]) {
            Mods.scanTick();
         } else {
            Mods.clearScan();
         }
      });

      WorldRenderEvents.LAST.register(Render::render);
      HudRenderCallback.EVENT.register(Hud::render);
   }

   /** Modulu ac/kapat (aksiyon modulleri calistirilir). */
   public static void toggle(int i) {
      if (i == BASES) {
         Mods.printBases();
         Toasts.push(NAMES[i], true, "Çalıştırıldı");
         return;
      }

      setOn(i, !on[i]);
      if (Config.toasts) {
         Toasts.push(NAMES[i], on[i], on[i] ? "Açıldı" : "Kapandı");
      } else if (mc.player != null) {
         mc.player.sendMessage(Text.literal("[Kuro] " + NAMES[i] + (on[i] ? " ACIK" : " KAPALI")), true);
      }
   }

   static void setOn(int i, boolean v) {
      if (on[i] == v) {
         return;
      }

      on[i] = v;
      if (i == FREECAM) {
         if (v) {
            Freecam.enable();
         } else {
            Freecam.disable();
         }
      } else if (i == FULLBRIGHT) {
         if (!v) {
            Extras.fullbrightOff();
         }
      } else if (i == FREELOOK) {
         if (v) {
            Freelook.enable();
         } else {
            Freelook.disable();
         }
      }
   }

   static Path keysFile() {
      return FabricLoader.getInstance().getGameDir().resolve("kuro_keys.txt");
   }

   public static void saveBinds() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < binds.length; i++) {
         sb.append(NAMES[i]).append('=').append(binds[i]).append('\n');
      }

      try {
         Files.writeString(keysFile(), sb.toString());
      } catch (IOException ignored) {
      }
   }

   static void loadBinds() {
      Path f = keysFile();
      if (Files.exists(f)) {
         try {
            for (String line : Files.readAllLines(f)) {
               String[] p = line.trim().split("=");
               if (p.length == 2) {
                  for (int i = 0; i < NAMES.length; i++) {
                     if (NAMES[i].equals(p[0].trim())) {
                        binds[i] = Integer.parseInt(p[1].trim());
                     }
                  }
               }
            }
         } catch (NumberFormatException | IOException ignored) {
         }
      }
   }
}
