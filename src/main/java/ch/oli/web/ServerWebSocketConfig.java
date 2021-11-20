package ch.oli.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ServerWebSocketConfig implements WebSocketConfigurer {
   @Override
   public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
       registry.addHandler(myServerWebSocketHandler(), "/pushChannel");
   }

   @Bean
   public ServerWebSocketHandler myServerWebSocketHandler() {
       return new ServerWebSocketHandler();
   }

//   @Bean
//   public TaskScheduler taskScheduler() {
//       ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//
//       scheduler.setPoolSize(2);
//       scheduler.setThreadNamePrefix("scheduled-task-");
//       scheduler.setDaemon(true);
//
//       return scheduler;
//   }
}