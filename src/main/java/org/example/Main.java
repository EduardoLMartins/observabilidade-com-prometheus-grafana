package org.example;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Main {

    Counter requestCounter =
            Counter.builder()
                    .name("aula_request_total")
                    .help("Contador de requests")
                    .labelNames("statusCode")
                    .register();

    Gauge freeBytes =
            Gauge.builder()
                    .name("aula_free_bytes")
                    .help("Exemplo de gauge")
                    .register();

    Histogram requestTime =
            Histogram.builder()
                    .name("aula_request_time_seconds")
                    .help("Tempo de resposta da API")
                    .classicUpperBounds(0.1, 0.2, 0.3, 0.4, 0.5)
                    .register();

    Summary summary =
            Summary.builder()
                    .name("aula_summary_request_time_seconds")
                    .quantile(0.5)
                    .quantile(0.99)
                    .quantile(0.9)
                    .help("Tempo de resposta da API")
                    .register();

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);

        // Mantém o servidor de métricas ativo
        HTTPServer server = HTTPServer.builder()
                .port(3000)
                .buildAndStart();

        System.out.println("Métricas disponíveis em http://127.0.0.1:8081/metrics");
    }

    @RestController
    class HelloController {
        @GetMapping("/")
        public String hello() {
            requestCounter.labelValues("200").inc();
            requestCounter.labelValues("500").inc();

            freeBytes.set(100 * Math.random());

            double tempo = Math.random();
            requestTime.observe(tempo);
            summary.observe(tempo);

            return "Hello World!";
        }
    }
}