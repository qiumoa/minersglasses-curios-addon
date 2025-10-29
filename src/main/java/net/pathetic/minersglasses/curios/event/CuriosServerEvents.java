package net.pathetic.minersglasses.curios.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.pathetic.minersglasses.curios.MinersGlassesCuriosAddon;
import net.pathetic.minersglasses.curios.networking.packet.CuriosXrayC2SPacket;
import net.pathetic.minersglasses.curios.util.CuriosHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * 服务端事件处理器 - 自动清除透视效果
 *
 * 原理: 使用独立的持续时间系统,越高级的眼镜透视持续越久
 * 冷却时间: 控制使用频率(越高级越短)
 * 持续时间: 控制透视效果显示时长(越高级越长)
 */
@Mod.EventBusSubscriber(modid = MinersGlassesCuriosAddon.MOD_ID)
public class CuriosServerEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 玩家透视状态跟踪类
     */
    private static class XrayState {
        String glassesType;          // 触发透视的眼镜类型
        long startTick;              // 透视开始的tick
        int duration;                // 透视持续时间(ticks)

        XrayState(String glassesType, long startTick, int duration) {
            this.glassesType = glassesType;
            this.startTick = startTick;
            this.duration = duration;
        }
    }

    /**
     * 记录每个玩家的透视状态
     * key: 玩家UUID, value: 透视状态
     */
    private static final java.util.Map<java.util.UUID, XrayState> playerXrayState =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 玩家tick事件 - 检测透视持续时间
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端执行
        if (event.side != LogicalSide.SERVER) {
            return;
        }

        if (!(event.player instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.player;
        java.util.UUID playerUUID = player.getUUID();

        // 检查玩家头盔槽是否有眼镜
        ItemStack helmetStack = player.getInventory().getArmor(3);
        boolean hasGlassesInHelmet = CuriosHelper.isMinersGlasses(helmetStack.getItem());

        // 如果头盔槽有眼镜,由原mod处理清除逻辑,我们不干预
        if (hasGlassesInHelmet) {
            // 清除Curios状态记录
            playerXrayState.remove(playerUUID);
            return;
        }

        // 检查是否有活跃的透视状态
        XrayState state = playerXrayState.get(playerUUID);
        if (state != null) {
            long currentTick = player.getServer().getTickCount();
            long elapsed = currentTick - state.startTick;

            // 检查持续时间是否已过
            if (elapsed >= state.duration) {
                LOGGER.info("玩家 {} 的 {} 眼镜透视持续时间结束 ({}秒),清除透视效果",
                    player.getName().getString(),
                    state.glassesType,
                    state.duration / 20.0);
                clearXrayDisplays(player);
                playerXrayState.remove(playerUUID);
            }
        }
    }

    /**
     * 注册透视状态 - 由外部调用(当玩家触发透视时)
     */
    public static void registerXray(ServerPlayer player, String glassesType) {
        int duration = CuriosXrayC2SPacket.getDurationForGlasses(glassesType);
        long startTick = player.getServer().getTickCount();

        XrayState state = new XrayState(glassesType, startTick, duration);
        playerXrayState.put(player.getUUID(), state);

        LOGGER.info("注册玩家 {} 的 {} 眼镜透视状态,持续时间: {}秒",
            player.getName().getString(),
            glassesType,
            duration / 20.0);
    }

    /**
     * 清除透视显示实体
     */
    private static void clearXrayDisplays(ServerPlayer player) {
        try {
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack().withSuppressedOutput(),
                "/kill @e[type=minecraft:block_display]"
            );
        } catch (Exception e) {
            LOGGER.error("清除透视效果时发生错误", e);
        }
    }
}
