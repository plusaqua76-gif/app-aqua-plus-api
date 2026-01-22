package com.aqua.plus.api.service;

import java.time.LocalDate;
import java.util.Map;

public interface IMetricasContablesService {

    Map<String, Object> obtenerMetricasContablesMap(Integer idEmpresa, Integer anio, Integer mes, 
            LocalDate fechaDesde, LocalDate fechaHasta, Integer cantidadPeriodos);

}
