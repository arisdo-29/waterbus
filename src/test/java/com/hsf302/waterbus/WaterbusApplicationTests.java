package com.hsf302.waterbus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Lưu ý: class test phải nằm CÙNG package (hoặc package con) với WaterbusApplication.
// Project mẫu của giảng viên để test ở package com.se0000.se2033_seldt_amg_pe khác với
// com.se0000.mvc nên @SpringBootTest không tìm thấy class main.
@SpringBootTest
class WaterbusApplicationTests {

    @Test
    void contextLoads() {
        // Khởi động toàn bộ app với H2 (src/test/resources/application.properties).
        // Entity map sai, thiếu @Service, trùng URL controller... đều làm test này fail.
    }

}
