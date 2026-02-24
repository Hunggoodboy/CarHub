package com.carhub.service.ai;

import com.carhub.dto.CarDTO;
import com.carhub.entity.Car;
import com.carhub.repository.CarRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VectorStoreService {
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
        metadata.put("model", car.getModel());
        metadata.put("color", car.getColor());
        metadata.put("price", car.getPrice());
        metadata.put("discount", car.getDiscount());
        metadata.put("stockQuantity", car.getStockQuantity());
        metadata.put("manufactureYear", car.getManufactureYear());
        return new Document(content, metadata);
    }
    private CarDTO documentToDTO(Document doc) {
        CarDTO carDTO = new CarDTO();
        Map<String, Object> metadata = doc.getMetadata();
        Optional.ofNullable(metadata.get("id"))
                .ifPresent(v -> carDTO.setId(Long.parseLong(v.toString())));

        Optional.ofNullable(metadata.get("model"))
                .ifPresent(v -> carDTO.setModel(v.toString()));

        Optional.ofNullable(metadata.get("color"))
                .ifPresent(v -> carDTO.setColor(v.toString()));

        Optional.ofNullable(metadata.get("price"))
                .ifPresent(v -> carDTO.setPrice(Double.parseDouble(v.toString())));

        Optional.ofNullable(metadata.get("discount"))
                .ifPresent(v -> carDTO.setDiscount(Double.parseDouble(v.toString())));

        Optional.ofNullable(metadata.get("stockQuantity"))
                .ifPresent(v -> carDTO.setStockQuantity((Integer) v));

        Optional.ofNullable(metadata.get("manufactureYear"))
                .ifPresent(v -> carDTO.setManufactureYear((Integer) v));
        return carDTO;
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
    public List<CarDTO> getCarsSimilar(Long id){
        Car car = carRepository.findById(id).orElse(null);
        String InformationOfCar = String.format("""
                Xe này tên là: %s,
                Thương hiệu xe này là : %s,
                Màu của xe là: %s,
                Miêu tả xe: %s,
                Xe có giá: %s,
                Xe được giảm giá: %s,
                Số xe còn lại là : %s,
                Xe được bảo hành tới năm: %s
                """, car.getName(), car.getModel(), car.getColor(), car.getDescription(), car.getPrice(), car.getDiscount(), car.getStockQuantity(), car.getManufactureYear());
        List<Document> cars = vectorStore.similaritySearch(SearchRequest.builder().query(InformationOfCar).topK(10).build());
        return cars.stream()
                .map(this::documentToDTO)
                .collect(Collectors.toList());
    }
}
