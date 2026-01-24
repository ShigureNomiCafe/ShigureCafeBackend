package cafe.shigure.ShigureCafeBackend.model;

public enum ReactionType {
    THUMBS_UP("👍"), // 点赞
    THUMBS_DOWN("👎"), // 踩 / 反对
    HEART("❤️"), // 红心 / 爱
    FIRE("🔥"), // 火热 / 热门
    PARTY("🎉"), // 庆祝 / 撒花
    CLAP("👏"), // 鼓掌
    GRIN("😁"), // 呲牙笑 (Telegram默认的笑脸)

    // --- 🥰 喜爱与开心 (Love & Happy) ---
    LOVING_FACE("🥰"), // 喜爱的脸 (带红心)
    STAR_EYES("🤩"), // 崇拜 / 追星
    JOY("😂"), // 笑哭
    ROFL("🤣"), // 笑得满地打滚
    RELIEVED("😌"), // 欣慰 / 松一口气
    SMILE("☺️"), // 羞涩微笑

    // --- 😲 惊讶与思考 (Shock & Thinking) ---
    SCREAM("😱"), // 惊恐 / 尖叫
    MIND_BLOWN("🤯"), // 脑炸 / 震撼
    EYES("👀"), // 关注 / 吃瓜 / 偷看
    THINKING("🤔"), // 思考
    SHOCK("😮"), // 张嘴惊讶
    WOW("😯"), // 哇

    // --- 🤡 嘲讽与梗文化 (Meme & Irony) ---
    // Telegram 很多群组活跃靠这组
    CLOWN("🤡"), // 小丑 / 傻瓜 / 自嘲
    POOP("💩"), // 便便 / 垃圾
    MONKEY_EYES("🙈"), // 非礼勿视 / 没眼看
    SHRUG("🤷"), // 耸肩 / 无奈 / 不知道
    NERD("🤓"), // 书呆子 / 专业 / 严谨

    // --- 😢 负面与批评 (Negative) ---
    CRY("😭"), // 大哭
    SAD("😢"), // 难过
    ANGRY("😡"), // 生气 (红脸)
    CURSING("🤬"), // 骂人 (嘴被堵住)
    VOMIT("🤮"), // 呕吐 / 恶心
    BORED("🥱"), // 打哈欠 / 无聊
    FACEPALM("🤦"), // 扶额 / 无语

    // --- ✅ 状态与标记 (Status & Objects) ---
    CHECK_MARK("✅"), // 完成 / 正确
    OK_HAND("👌"), // OK / 好的
    SALUTE("🫡"), // 敬礼 (收到指令)
    HANDSHAKE("🤝"), // 握手 / 合作
    PRAY("🙏"), // 祈祷 / 拜托
    HUNDRED("💯"), // 满分
    ROCKET("🚀"), // 发射 / 冲
    LIGHT_BULB("💡"), // 灵感 / 想法
    MOON_FACE("🌚"); // 月亮脸 (滑稽/阴险)

    private final String emoji;

    ReactionType(String emoji) {
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }
}
