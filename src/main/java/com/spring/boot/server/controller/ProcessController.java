package com.spring.boot.server.controller;

import com.spring.boot.server.starter.ProcessService;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

@RestController
@RequiredArgsConstructor
public class ProcessController {

  @Autowired
  private final ProcessService processService;

  @GetMapping(value = "/start-server")
  @ResponseStatus(HttpStatus.OK)
  public void startServer()
      throws IOException, ParserConfigurationException, SAXException {
    processService.starter();
  }

  @GetMapping(value = "/stop-server/{pid}")
  @ResponseStatus(HttpStatus.OK)
  public void stopServer(@PathVariable Long pid) {
    processService.kill(pid);
  }

  @GetMapping(value = "/get-info/{pid}")
  @ResponseStatus(HttpStatus.OK)
  public void getInfo(@PathVariable Long pid) {
    processService.getInfo(pid);
  }
}