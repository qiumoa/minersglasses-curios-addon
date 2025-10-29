package net.pathetic.minersglasses.curios.util;

import net.minecraft.client.KeyMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * 按键绑定工具类 - 通过反射读取原mod的按键设置
 */
public class KeyBindingHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static KeyMapping glassesAbilityKey = null;
    private static boolean initialized = false;

    // 可能的类名和字段名组合
    private static final String[][] POSSIBLE_CONFIGS = {
        {"net.pathetic.minersglasses.util.ModKeyBinding", "GLASSES_ABILITY_KEY"},
        {"net.pathetic.minersglasses.client.ModKeyBinding", "GLASSES_ABILITY_KEY"},
        {"net.pathetic.minersglasses.MinerGlassesClient", "keyXray"},
        {"net.pathetic.minersglasses.client.MinerGlassesClient", "keyXray"},
        {"net.pathetic.minersglasses.client.ClientHandler", "keyXray"},
        {"net.pathetic.minersglasses.KeyBindings", "GLASSES_ABILITY_KEY"}
    };

    /**
     * 初始化并获取原mod的按键绑定
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        LOGGER.info("========================================");
        LOGGER.info("开始尝试加载原mod的按键绑定...");

        // 尝试所有可能的配置
        for (String[] config : POSSIBLE_CONFIGS) {
            String className = config[0];
            String fieldName = config[1];

            LOGGER.info("尝试: 类 = {}, 字段 = {}", className, fieldName);

            try {
                Class<?> targetClass = Class.forName(className);
                Field keyField = targetClass.getDeclaredField(fieldName);
                keyField.setAccessible(true);

                Object fieldValue = keyField.get(null);

                if (fieldValue instanceof KeyMapping) {
                    glassesAbilityKey = (KeyMapping) fieldValue;
                    LOGGER.info("========================================");
                    LOGGER.info("✓ 成功加载按键绑定!");
                    LOGGER.info("  类名: {}", className);
                    LOGGER.info("  字段: {}", fieldName);
                    LOGGER.info("  按键: {}", glassesAbilityKey.getKey().getValue());
                    LOGGER.info("========================================");
                    initialized = true;
                    return;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.debug("  → 类不存在: {}", className);
            } catch (NoSuchFieldException e) {
                LOGGER.debug("  → 字段不存在: {} 在 {}", fieldName, className);
            } catch (IllegalAccessException e) {
                LOGGER.warn("  → 无法访问字段: {} 在 {}", fieldName, className);
            } catch (Exception e) {
                LOGGER.warn("  → 发生错误: {}", e.getMessage());
            }
        }

        LOGGER.error("========================================");
        LOGGER.error("✗ 所有尝试都失败了!");
        LOGGER.error("请确保已安装 Miner's Glasses 原mod");
        LOGGER.error("========================================");
        initialized = true;
    }

    /**
     * 获取原mod的按键绑定
     * @return KeyMapping 对象，如果获取失败返回 null
     */
    public static KeyMapping getGlassesAbilityKey() {
        if (!initialized) {
            initialize();
        }
        return glassesAbilityKey;
    }

    /**
     * 检查按键是否被按下
     * @return true 如果按键被按下
     */
    public static boolean isKeyPressed() {
        if (!initialized) {
            initialize();
        }

        if (glassesAbilityKey != null) {
            return glassesAbilityKey.isDown();
        }

        // 如果反射失败，记录警告
        LOGGER.warn("按键检测失败：无法读取原mod的按键设置");
        return false;
    }

    /**
     * 检查指定的按键事件是否匹配原mod的按键绑定
     * @param key 按键代码
     * @param action 按键动作
     * @return true 如果匹配
     */
    public static boolean matchesKeyBinding(int key, int action) {
        if (!initialized) {
            initialize();
        }

        if (glassesAbilityKey != null) {
            // 检查按键是否匹配并且是按下动作
            return glassesAbilityKey.matches(key, action);
        }

        return false;
    }
}
