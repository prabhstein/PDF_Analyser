package com.prabhstein.pdfanalyser.web;

import com.prabhstein.pdfanalyser.chat.ChatService;
import com.prabhstein.pdfanalyser.document.DocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DocumentController {

    private final DocumentService documentService;
    private final ChatService chatService;

    public DocumentController(DocumentService documentService, ChatService chatService) {
        this.documentService = documentService;
        this.chatService = chatService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("documents", documentService.findAll());
        return "index";
    }

    @PostMapping("/documents")
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        documentService.upload(file);
        model.addAttribute("documents", documentService.findAll());
        return "fragments :: documentList";
    }

    @GetMapping("/documents/{id}")
    public String document(@PathVariable long id, Model model) {
        model.addAttribute("document", documentService.findById(id));
        model.addAttribute("chunkCount", documentService.countChunks(id));
        model.addAttribute("messages", chatService.history(id));
        return "fragments :: documentWorkspace";
    }

    @GetMapping("/documents/{id}/page")
    public String documentPage(@PathVariable long id, Model model) {
        model.addAttribute("documents", documentService.findAll());
        model.addAttribute("document", documentService.findById(id));
        model.addAttribute("chunkCount", documentService.countChunks(id));
        model.addAttribute("messages", chatService.history(id));
        return "index";
    }

    @PostMapping("/documents/{id}/questions")
    public String ask(@PathVariable long id, @RequestParam("question") String question, Model model) {
        chatService.ask(id, question);
        model.addAttribute("messages", chatService.history(id));
        return "fragments :: chatHistory";
    }
}
