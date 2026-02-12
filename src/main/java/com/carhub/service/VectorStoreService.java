package com.carhub.service;

import com.carhub.dto.CarDTO;
import com.carhub.entity.Car;
import com.carhub.repository.CarRepository;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.List;

@Service
public class VectorStoreService {
    private final VectorStore vectorStore;
    private final File VectorFile = new File("carhub_vector_data.json");
    @Autowired
    private CarRepository carRepository;
    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        if(VectorFile.exists()) {
            ((SimpleVectorStore)vectorStore).load(VectorFile);
        }
    }
    public void addDocument(Car car) {
    }
}
