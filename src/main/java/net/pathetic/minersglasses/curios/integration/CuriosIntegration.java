package net.pathetic.minersglasses.curios.integration;

import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Curios 集成类
 * 注册眼镜物品到 Curios 饰品槽位
 *
 * 注意：从 Curios 5.7+ 开始，不再需要通过代码注册槽位
 * 槽位注册应该通过数据包（datapack）完成
 * 参见：src/main/resources/data/curios/tags/items/head.json
 */
public class CuriosIntegration {

    /**
     * 在 InterModEnqueue 阶段注册 Curios 槽位类型
     *
     * @deprecated Curios 5.7+ 已不再使用 InterModComms 注册槽位
     * 现在使用数据包（datapack）方式：data/curios/tags/items/head.json
     */
    @Deprecated
    public static void registerCuriosSlots(InterModEnqueueEvent event) {
        // Curios 5.7+ 不再需要此方法
        // 槽位通过 data/curios/tags/items/head.json 自动注册
    }
}
