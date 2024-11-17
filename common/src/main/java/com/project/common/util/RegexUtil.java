package com.project.common.util;

public class RegexUtil {

    /**
     * 電子郵件正則表達式：
     * - 格式說明：必須包含「@」和域名。
     * - 本地部分允許使用字母、數字、點 (`.`)、底線 (`_`)、百分號 (`%`)、加號 (`+`)、減號 (`-`) 及其他合法符號。
     * - 領域部分允許字母、數字和減號 (`-`)。
     * - 結尾必須為至少兩個字母的域名後綴（如 `.com`、`.net`）。
     * - 總長度限制為 3 到 50 個字元。
     */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    /**
     * 密碼正則表達式：
     * - 格式說明：密碼長度必須在 8 到 15 個字元之間。
     * - 必須同時包含以下條件：
     *   1. 至少一個小寫英文字母（`a-z`）
     *   2. 至少一個大寫英文字母（`A-Z`）
     *   3. 至少一個數字（`0-9`）
     * - 禁止使用空格、特殊字元或超出範圍的符號。
     */
    public static final String PASSWORD_REGEX = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,15}$";


}
