package com.example.demo.services;

import com.example.demo.entities.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QdrantService {

    private final VectorStore vectorStore;

    @Value("${app.ai.qdrant.search-top-k:5}")
    private int searchTopK;

    /**
     * Day thong tin san pham len Qdrant (Upsert)
     */
    public void upsertProduct(Product product) {
        try {
            String safePrice = formatPrice(product.getPrice());
            // Tạo chuỗi nội dung cực kỳ chi tiết bao gồm tất cả thuộc tính
            String content = String.format(
                    "Sản phẩm: %s. Thương hiệu: %s. Trạng thái: %s. Số lượng còn lại trong kho: %d. " +
                            "Giá bán: %s VND. Loại máy: %s. Mặt kính: %s. Kháng nước: %s. Kích thước mặt: %s. " +
                            "Chất liệu dây: %s. Màu dây: %s. Màu vỏ: %s. Màu mặt: %s. " +
                            "Mô tả chi tiết: %s",
                    product.getName(),
                    product.getBrand(),
                    product.getStatus() != null ? product.getStatus().name() : "Đang bán",
                    product.getStockQuantity(), // Quan trọng: Thêm số lượng tồn kho vào đây
                    safePrice,
                    product.getMovementType(),
                    product.getGlassMaterial(),
                    product.getWaterResistance(),
                    product.getFaceSize(),
                    product.getWireMaterial(),
                    product.getWireColor(),
                    product.getCaseColor(),
                    product.getFaceColor(),
                    product.getDescription()
            );

            Document document = new Document(
                    product.getId(),
                    content,
                    Map.of(
                            "productId", product.getId(),
                            "brand", product.getBrand(),
                            "price", product.getPrice(),
                            "stock", product.getStockQuantity(), // Thêm vào metadata để lọc nếu cần
                            "status", product.getStatus() != null ? product.getStatus().name() : "ACTIVE"
                    )
            );

            vectorStore.add(List.of(document));
            log.info("Successfully synced product {} to Qdrant with stock: {}", product.getId(), product.getStockQuantity());
        } catch (Exception ex) {
            log.error("Failed to upsert product {} to Qdrant", product.getId(), ex);
        }
    }

    /**
     * Xoa san pham khoi Qdrant
     */
    public void deleteProduct(String productId) {
        try {
            vectorStore.delete(List.of(productId));
        } catch (Exception ex) {
            log.error("Failed to delete product {} from Qdrant", productId, ex);
        }
    }

    /**
     * Tim kiem cac san pham co dac diem tuong dong voi cau hoi cua khach
     */
    public String searchRelevantContext(String query) {
        try {
            int topK = Math.max(1, searchTopK);
            List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build()
            );

            return results.stream()
                .map(Document::getText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n---\n"));
        } catch (Exception ex) {
            log.error("Failed to search context from Qdrant for query: {}", query, ex);
            return "";
        }
    }

    private String formatPrice(Object price) {
        if (price == null) {
            return "khong ro";
        }
        if (price instanceof Number number) {
            return String.format("%,.0f", number.doubleValue());
        }
        return String.valueOf(price);
    }
}
