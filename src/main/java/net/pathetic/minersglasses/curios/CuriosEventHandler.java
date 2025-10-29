package net.pathetic.minersglasses.curios;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.pathetic.minersglasses.curios.networking.ModMessages;
import net.pathetic.minersglasses.curios.networking.packet.CuriosXrayC2SPacket;
import net.pathetic.minersglasses.curios.util.CuriosHelper;
import net.pathetic.minersglasses.curios.util.KeyBindingHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * 客户端事件处理器 - 监听按键输入
 */
@Mod.EventBusSubscriber(modid = MinersGlassesCuriosAddon.MOD_ID, value = Dist.CLIENT)
public class CuriosEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean keyBindingInitialized = false;

    /**
     * 监听按键输入事件
     * 动态读取原mod的按键绑定，而非硬编码H键
     *
     * 使用 LOWEST 优先级，确保在原mod处理后才检查
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) {
            return;
        }

        // 延迟初始化按键绑定(确保原mod已加载)
        if (!keyBindingInitialized) {
            KeyBindingHelper.initialize();
            keyBindingInitialized = true;
        }

        // 使用反射动态检测原mod的按键绑定
        if (KeyBindingHelper.matchesKeyBinding(event.getKey(), event.getAction())) {
            LOGGER.info("检测到按键按下! Key={}, Action={}", event.getKey(), event.getAction());

            // 检查玩家头盔槽位是否有眼镜
            ItemStack helmetStack = player.getInventory().getArmor(3);
            boolean hasGlassesInHelmet = CuriosHelper.isMinersGlasses(helmetStack.getItem());

            String helmetId = helmetStack.isEmpty() ? "N/A" :
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(helmetStack.getItem()).toString();

            LOGGER.info("头盔槽位: {} (物品ID: {}, 是否为眼镜: {})",
                helmetStack.isEmpty() ? "空" : helmetStack.getItem(),
                helmetId,
                hasGlassesInHelmet);

            // **关键**: 如果头盔槽有眼镜，不做任何处理，让原mod处理
            if (hasGlassesInHelmet) {
                LOGGER.info("头盔槽位有眼镜，由原mod处理，跳过附属mod逻辑");
                return; // 直接返回，不干预原mod
            }

            // 只有头盔槽位没有眼镜时，才检查 Curios 槽位
            Optional<ItemStack> curiosGlasses = CuriosHelper.findGlassesInCurios(player);
            boolean hasGlassesInCurios = curiosGlasses.isPresent();

            if (hasGlassesInCurios) {
                ItemStack glassesStack = curiosGlasses.get();
                String glassesId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(glassesStack.getItem()).toString();

                LOGGER.info("Curios 槽位: {} (物品ID: {}, 是否为眼镜: {})",
                    glassesStack.getItem(),
                    glassesId,
                    true);

                LOGGER.info("从 Curios 槽位触发透视，发送数据包到服务端...");
                // 发送数据包到服务端
                ModMessages.sendToServer(new CuriosXrayC2SPacket());
            } else {
                LOGGER.warn("Curios 槽位中没有找到眼镜!");
            }
        }
    }
}
