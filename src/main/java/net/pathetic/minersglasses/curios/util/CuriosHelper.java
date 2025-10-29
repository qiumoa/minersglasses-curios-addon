package net.pathetic.minersglasses.curios.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Curios 工具类 - 用于检查和获取饰品槽位中的眼镜
 */
public class CuriosHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 检查物品是否为矿工眼镜
     */
    public static boolean isMinersGlasses(Item item) {
        if (item == null) return false;

        // 使用 ResourceLocation 获取正确的物品ID
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).toString();

        LOGGER.debug("检查物品ID: {}", itemId);

        // 检查所有类型的矿工眼镜
        return itemId.equals("minersglasses:coal_glasses")
                || itemId.equals("minersglasses:lapis_glasses")
                || itemId.equals("minersglasses:copper_glasses")
                || itemId.equals("minersglasses:iron_glasses")
                || itemId.equals("minersglasses:redstone_glasses")
                || itemId.equals("minersglasses:gold_glasses")
                || itemId.equals("minersglasses:emerald_glasses")
                || itemId.equals("minersglasses:diamond_glasses")
                || itemId.equals("minersglasses:netherite_glasses")
                || itemId.equals("minersglasses:glasses");
    }

    /**
     * 从 Curios 饰品槽位中查找矿工眼镜
     * @param player 玩家
     * @return 找到的眼镜物品栈（如果有）
     */
    public static Optional<ItemStack> findGlassesInCurios(Player player) {
        return CuriosApi.getCuriosInventory(player).resolve().flatMap(inventory -> {
            // 查找所有矿工眼镜（使用 Lambda 表达式匹配 Predicate<ItemStack>）
            List<SlotResult> results = inventory.findCurios(stack -> isMinersGlasses(stack.getItem()));

            if (!results.isEmpty()) {
                return Optional.of(results.get(0).stack());
            }
            return Optional.empty();
        });
    }

    /**
     * 检查玩家是否在任何位置装备了矿工眼镜（头盔槽或饰品槽）
     * @param player 玩家
     * @return 是否装备了眼镜
     */
    public static boolean hasGlassesEquipped(Player player) {
        // 检查头盔槽位
        ItemStack helmet = player.getInventory().getArmor(3);
        if (isMinersGlasses(helmet.getItem())) {
            return true;
        }

        // 检查 Curios 饰品槽位
        return findGlassesInCurios(player).isPresent();
    }

    /**
     * 获取玩家装备的矿工眼镜（优先头盔槽，其次饰品槽）
     * @param player 玩家
     * @return 眼镜物品栈
     */
    public static ItemStack getEquippedGlasses(Player player) {
        // 优先检查头盔槽位
        ItemStack helmet = player.getInventory().getArmor(3);
        if (isMinersGlasses(helmet.getItem())) {
            return helmet;
        }

        // 检查 Curios 饰品槽位
        return findGlassesInCurios(player).orElse(ItemStack.EMPTY);
    }
}
