package com.exemple.historique.controller;

import com.exemple.historique.client.CalculClient;
import com.exemple.historique.model.Operation;
import com.exemple.historique.service.OperationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {

    private static final List<String> OPERATEURS =
            List.of("somme", "soustraction", "produit", "division");

    private final OperationService operationService;
    private final CalculClient calculClient;

    // Demande le résultat à CALCUL-SERVICE (instance choisie par LoadBalancer)
    // et le range dans l'opération. Retourne une réponse d'erreur si le calcul
    // est impossible, null sinon.
    private ResponseEntity<Object> calculer(Operation op) {
        if (op.getOperateur() == null || !OPERATEURS.contains(op.getOperateur())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erreur", "Operateur inconnu : " + op.getOperateur()));
        }
        try {
            String corps = calculClient.calculer(op.getOperateur(), op.getA(), op.getB());
            op.setResultat(Double.parseDouble(corps));
            return null;
        } catch (FeignException.BadRequest e) {
            // calcul-service a répondu 400 (division par 0) : on transmet le message
            return ResponseEntity.badRequest()
                    .body(Map.of("erreur", e.contentUTF8()));
        } catch (FeignException e) {
            // Aucune instance de CALCUL-SERVICE disponible ou injoignable
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erreur", "calcul-service injoignable"));
        }
    }

    // POST /operations
    @PostMapping
    public ResponseEntity<Object> creer(@RequestBody Operation op) {
        ResponseEntity<Object> erreur = calculer(op);
        if (erreur != null) {
            return erreur;
        }
        Operation creee = operationService.ajouter(op);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creee.getId())
                .toUri();
        return ResponseEntity.created(location).body(creee);
    }

    // GET /operations
    @GetMapping
    public List<Operation> lister() {
        return operationService.lister();
    }

    // GET /operations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Operation> lire(@PathVariable("id") int id) {
        Operation op = operationService.lire(id);
        if (op == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(op);
    }

    // PUT /operations/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Object> modifier(@PathVariable("id") int id,
                                           @RequestBody Operation op) {
        if (!operationService.existe(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erreur", "Opération introuvable : " + id));
        }
        ResponseEntity<Object> erreur = calculer(op);
        if (erreur != null) {
            return erreur;
        }
        operationService.remplacer(id, op);
        return ResponseEntity.ok(op);
    }

    // DELETE /operations/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> supprimer(@PathVariable("id") int id) {
        if (operationService.supprimer(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erreur", "Opération introuvable : " + id));
        }
        return ResponseEntity.noContent().build();
    }
}