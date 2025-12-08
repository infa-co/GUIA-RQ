package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoucherServiceImpl implements VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Override
    public boolean usuarioPossuiVoucherAtivo(String cpf) {
        return voucherRepository.existsByCpfClienteAndUsadoFalse(cpf);
    }
}
