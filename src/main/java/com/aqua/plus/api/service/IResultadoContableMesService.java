package com.aqua.plus.api.service;

import java.time.LocalDate;
import java.util.Map;

public interface IResultadoContableMesService {

    Map<String, Object> obtenerResultadoContableMesMap(Integer idEmpresa, Integer anio, Integer mes,  LocalDate fechaDesde, LocalDate fechaHasta);
    
}
