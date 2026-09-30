package io.github.rbrisuda.jevface.examples.support;

import io.github.rbrisuda.jevface.JevEvaluationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TicketController {

    record Ticket(String message) {}

    private final TicketRouter router;

    TicketController(TicketRouter router) {
        this.router = router;
    }

    @PostMapping("/tickets")
    RoutingDecision route(@RequestBody Ticket ticket) {
        return router.route(ticket.message());
    }

    @ExceptionHandler
    ProblemDetail jevUnavailable(JevEvaluationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}
