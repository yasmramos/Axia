package com.axia.service;

import com.axia.model.Supplier;
import com.axia.repository.SupplierRepository;

import java.util.List;
import java.util.Optional;

public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService() {
        this.supplierRepository = new SupplierRepository();
    }

    public Supplier create(String code, String name, String taxId, String address,
                           String city, String phone, String email) {
        if (supplierRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Ya existe un proveedor con el código: " + code);
        }

        Supplier supplier = new Supplier();
        supplier.setCode(code);
        supplier.setName(name);
        supplier.setTaxId(taxId);
        supplier.setAddress(address);
        supplier.setCity(city);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setActive(true);

        supplierRepository.save(supplier);
        return supplier;
    }

    public Supplier update(Supplier supplier) {
        supplierRepository.update(supplier);
        return supplier;
    }

    public void delete(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        if (!supplier.getInvoices().isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar un proveedor con facturas asociadas");
        }

        supplierRepository.delete(supplier);
    }

    public void deactivate(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        supplier.setActive(false);
        supplierRepository.update(supplier);
    }

    public Optional<Supplier> findById(Long id) {
        return supplierRepository.findById(id);
    }

    public Optional<Supplier> findByCode(String code) {
        return supplierRepository.findByCode(code);
    }

    public List<Supplier> findAll() {
        return supplierRepository.findAll();
    }

    public List<Supplier> findActive() {
        return supplierRepository.findActive();
    }

    public List<Supplier> search(String term) {
        return supplierRepository.search(term);
    }
}
