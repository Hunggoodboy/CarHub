package com.carhub.service.ai;

import com.carhub.entity.Car;
import com.carhub.repository.CarRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import javax.print.Doc;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VectorStoreService {
//    private final File vectorFile = new File("carhub_vector_data.json");
    private final CarRepository carRepository;
    private final VectorStore vectorStore;
    //Chuyển từ Car đổi thành Document
    private Document carToDocument(Car car) {
        String content = String.format("""
                Xe này tên là: %s,
                Thương hiệu xe này là : %s,
                Màu của xe là: %s,
                Miêu tả xe: %s,
                Xe có giá: %s,
                Xe được giảm giá: %s,
                Số xe còn lại là : %s,
                Xe được bảo hành tới năm: %s
                """, car.getName(), car.getModel(), car.getColor(), car.getDescription(), car.getPrice(), car.getDiscount(), car.getStockQuantity(), car.getManufactureYear());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id",  car.getId());
        metadata.put("name", car.getName());
        metadata.put("model", car.getModel());
        metadata.put("color", car.getColor());
        metadata.put("year", car.getManufactureYear());
        metadata.put("price", car.getPrice());
        return new Document(content, metadata);
    }
    @PostConstruct
    public void loadAllCars() {
        boolean vectorEmpty = vectorStore.similaritySearch(SearchRequest.builder().query("Xe").topK(1).build()).isEmpty();
        if(vectorEmpty) {
            List<Car> cars = carRepository.findAll();
            List<Document> documentList = cars.stream()
                    .map(this::carToDocument)
                    .collect(Collectors.toList());
            vectorStore.add(documentList);
            System.out.println(String.format("Đã nạp %s dữ liệu  xe vào VectorStore",  documentList.size()));
        }
    }
    public void loadCar(Car car) {
        Document docCar = carToDocument(car);
        List<Document> toList = new ArrayList<>();
        toList.add(docCar);
        vectorStore.add(toList);
    }
}
