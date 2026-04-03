package com.example.demo.services;

import com.example.demo.entities.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QdrantService {

    private final VectorStore vectorStore;

    /**
     * Đẩy thông tin sản phẩm lên Qdrant (Upsert)
     */
    public void upsertProduct(Product product) {
        // Tạo nội dung văn bản để AI hiểu về sản phẩm
        String content = String.format(
            "Đồng hồ: %s. Thương hiệu: %s. Giá: %d VNĐ. Chất liệu: %s. Mô tả: %s",
            product.getName(), product.getBrand(), product.getPrice(), 
            product.getWireMaterial(), product.getDescription()
        );

        // Tạo Document kèm metadata để truy xuất sau này
        Document document = new Document(
            product.getId(), // ID từ DB SQL
            content,
            Map.of(
                "productId", product.getId(),
                "brand", product.getBrand(),
                "price", product.getPrice()
            )
        );

        vectorStore.add(List.of(document));
    }

    /**
     * Xóa sản phẩm khỏi Qdrant
     */
    public void deleteProduct(String productId) {
        vectorStore.delete(List.of(productId));
    }

    /**
     * Tìm kiếm các sản phẩm có đặc điểm tương đồng với câu hỏi của khách
     */
    public String searchRelevantContext(String query) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(3).build()
        );

        // Gộp kết quả thành một chuỗi văn bản làm ngữ cảnh (Context) cho AI
        return results.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }
}
