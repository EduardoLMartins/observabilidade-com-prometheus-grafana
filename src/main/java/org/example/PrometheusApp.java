package org.example;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.Histogram;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

// Classe principal da aplicação Spring Boot que expõe métricas para Prometheus
@SpringBootApplication
@RestController
public class PrometheusApp {

    private final Counter requests200;
    private final Counter requests500;
    private final AtomicInteger usuariosLogados;
    private final MeterRegistry registry;
    private final Random random = new Random();
    private volatile boolean zeraUsuariosLogados = false;

    // Construtor da aplicação
    // Aqui são inicializados os contadores, gauge e histograma que serão expostos como métricas
    public PrometheusApp(MeterRegistry registry) {
        this.registry = registry;

        // Contador de requisições com status 200 (sucesso)
        this.requests200 = Counter.builder("aula_requests_total")
                .tag("statusCode", "200")
                .description("Contador de requests com sucesso")
                .register(registry);

        // Contador de requisições com status 500 (erro)
        this.requests500 = Counter.builder("aula_requests_total")
                .tag("statusCode", "500")
                .description("Contador de requests com erro")
                .register(registry);

        // Gauge que representa o número de usuários logados em tempo real
        this.usuariosLogados = new AtomicInteger(0);
        Gauge.builder("aula_usuarios_logados_total", usuariosLogados, AtomicInteger::get)
                .description("Número de usuários logados no momento")
                .register(registry);

        // Histograma para medir o tempo de resposta da API
        Histogram.build()
                .name("aula_request_duration_seconds")
                .help("Tempo de resposta da API")
                .register();
    }

    // Método chamado automaticamente após a inicialização da aplicação
    // Cria uma thread que simula métricas continuamente (requests, usuários logados, tempo de resposta)
    @PostConstruct
    public void startMetricsSimulation() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(150);

                    // Simula requisições: 5% de erro, 95% de sucesso
                    int taxaDeErro = 5;
                    if (random.nextDouble() < taxaDeErro / 100.0) {
                        requests500.increment();
                    } else {
                        requests200.increment();
                    }

                    // Atualiza o gauge de usuários logados
                    int usuarios = (zeraUsuariosLogados) ? 0 : 500 + random.nextInt(50);
                    usuariosLogados.set(usuarios);

                    // Observa tempo de resposta simulando distribuição normal enviesada
                    double tempoObservado = randn_bm(0, 3, 4);
                    registry.timer("aula_request_duration_seconds")
                            .record((long) (tempoObservado * 1000), java.util.concurrent.TimeUnit.MILLISECONDS);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    // Função auxiliar que gera números aleatórios com distribuição normal enviesada
    // Usada para simular tempos de resposta mais realistas
    private double randn_bm(double min, double max, double skew) {
        double u = 0, v = 0;
        while (u == 0) u = random.nextDouble();
        while (v == 0) v = random.nextDouble();
        double num = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
        num = num / 10.0 + 0.5;
        if (num > 1 || num < 0) return randn_bm(min, max, skew);
        num = Math.pow(num, skew);
        num *= (max - min);
        num += min;
        return num;
    }

    // Endpoint GET "/" que retorna uma mensagem simples "Hello World!"
    @GetMapping("/")
    public String hello() {
        return "Hello World!";
    }

    // Endpoint GET "/zera-usuarios-logados"
    // Zera o número de usuários logados (simulação)
    @GetMapping("/zera-usuarios-logados")
    public String zeraUsuarios() {
        zeraUsuariosLogados = true;
        return "OK";
    }

    // Endpoint GET "/retorna-usuarios-logados"
    // Restaura o número de usuários logados para valores simulados
    @GetMapping("/retorna-usuarios-logados")
    public String retornaUsuarios() {
        zeraUsuariosLogados = false;
        return "OK";
    }

    // Método main que inicia a aplicação Spring Boot
    public static void main(String[] args) {
        SpringApplication.run(PrometheusApp.class, args);
    }
}