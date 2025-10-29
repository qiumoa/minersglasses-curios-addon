package net.pathetic.minersglasses.curios;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.pathetic.minersglasses.curios.event.CuriosServerEvents;
import net.pathetic.minersglasses.curios.networking.ModMessages;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 矿工眼镜 Curios 附属模组主类
 * 为矿工眼镜添加 Curios 饰品栏支持
 */
@Mod("minersglasses_curios_addon")
public class MinersGlassesCuriosAddon {
    public static final String MOD_ID = "minersglasses_curios_addon";
    private static final Logger LOGGER = LogManager.getLogger();

    public MinersGlassesCuriosAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册通用设置事件
        modEventBus.addListener(this::commonSetup);

        // 注册客户端事件处理器
        MinecraftForge.EVENT_BUS.register(new CuriosEventHandler());

        // 注册服务端事件处理器 (自动清除透视效果)
        MinecraftForge.EVENT_BUS.register(CuriosServerEvents.class);

        LOGGER.info("========================================");
        LOGGER.info("矿工眼镜 Curios 附属模组已加载");
        LOGGER.info("版本: 1.0.4 | 作者: QiuMo");
        LOGGER.info("功能: 支持将眼镜放入饰品槽并使用透视");
        LOGGER.info("注意: Curios 槽位通过数据包自动注册 (data/curios/tags/items/head.json)");
        LOGGER.info("========================================");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 注册网络消息
        event.enqueueWork(() -> {
            ModMessages.register();
            LOGGER.info("网络消息通道注册完成");
        });
    }
}
