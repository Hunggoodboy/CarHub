package com.carhub.service.ai;

import com.carhub.dto.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AIChatService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    public AIChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public String generateAnswer(ChatRequest chatRequest) throws InterruptedException {
        List<Document> similarDocument = vectorStore.similaritySearch(
                SearchRequest.builder().query(chatRequest.getQuestion())
                        .topK(4)
                        .build()
        );
        String context = similarDocument.stream().map(Document::getText).collect(Collectors.joining("\n"));
        String promptTemplate = String.format("""
                Bạn là trợ lý ảo cho shop CarHub,
                Dưới đây là các thông tin trả lời cho khách(Context):
                %s
                Bạn hãy là giúp tôi tư vấn cho khách hàng những chiếc xe phù hợp nhé
                Bạn chỉ nên trả lời đúng câu hỏi của khách hàng chứ đừng trả lời miên man
                """, context);
        try {
            return chatClient.prompt().user(promptTemplate).call().content();
        }
        catch (Exception e) {
            if (e.getMessage().contains("429")) {
                Thread.sleep(15000); // chờ 15 giây
                return chatClient.prompt().user(promptTemplate).call().content();
            }
            throw e;
        }
    }
}
