// Interactividad puntual del lado del cliente (seccion 4.4/4.7): esta app es
// server-rendered (Thymeleaf); este script NO consume ningun servicio externo.
document.addEventListener("DOMContentLoaded", function () {
  // --- Toasts: auto-dismiss (4 seg success/info, 6 seg warning, persistente error) ---
  document.querySelectorAll(".toast").forEach(function (toast) {
    var tipo = toast.dataset.tipo;
    if (tipo === "error") return; // requiere cierre manual
    var duracion = tipo === "warning" ? 6000 : 4000;
    setTimeout(function () {
      toast.style.transition = "opacity 200ms ease";
      toast.style.opacity = "0";
      setTimeout(function () { toast.remove(); }, 200);
    }, duracion);
  });
  document.querySelectorAll(".toast-cerrar").forEach(function (btn) {
    btn.addEventListener("click", function () { btn.closest(".toast").remove(); });
  });

  // --- Deshacer: cuenta regresiva de 10 segundos (IMPLEMENTACION.md 4.2) ---
  document.querySelectorAll(".toast[data-undo-id]").forEach(function (toast) {
    var uuid = toast.dataset.undoId;
    if (!uuid) return;
    var contador = toast.querySelector(".contador-deshacer");
    var btnDeshacer = toast.querySelector(".btn-deshacer");
    var restante = 10;
    var intervalo = setInterval(function () {
      restante -= 1;
      if (contador) contador.textContent = restante + "s";
      if (restante <= 0) {
        clearInterval(intervalo);
        if (btnDeshacer) btnDeshacer.disabled = true;
      }
    }, 1000);
    if (btnDeshacer) {
      btnDeshacer.addEventListener("click", function () {
        clearInterval(intervalo);
        btnDeshacer.disabled = true;
        var params = new URLSearchParams();
        params.set(toast.dataset.csrfParam, toast.dataset.csrfToken);
        fetch("/deshacer/" + uuid, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: params.toString() })
          .then(function (r) { return r.json(); })
          .then(function (data) { if (data.ok) window.location.reload(); });
      });
    }
  });

  // --- Combos de busqueda (input + datalist) que resuelven a un id oculto:
  //     <input list="..." data-busqueda-id="idDelHidden"> + <input type="hidden" id="idDelHidden">.
  //     Permite escribir para filtrar en listas largas (ej. docentes) en vez de un <select> gigante. ---
  document.querySelectorAll("input[data-busqueda-id]").forEach(function (input) {
    var hidden = document.getElementById(input.dataset.busquedaId);
    var datalist = document.getElementById(input.getAttribute("list"));
    if (!hidden || !datalist) return;
    function sincronizar() {
      var opciones = datalist.querySelectorAll("option");
      var encontrada = Array.prototype.find.call(opciones, function (o) { return o.value === input.value; });
      hidden.value = encontrada ? encontrada.dataset.id : "";
    }
    input.addEventListener("input", sincronizar);
    sincronizar();
  });

  // --- Resalta el item de navegacion activo segun la URL actual (seccion 4.4) ---
  document.querySelectorAll(".sidebar a").forEach(function (link) {
    var href = link.getAttribute("href");
    if (href && href !== "/" && location.pathname.indexOf(href) === 0) {
      link.classList.add("activo");
    }
  });

  // --- Sidebar deslizable en celular y tablet (hasta 1023px): se cierra tocando
  //     afuera, eligiendo una opcion o con Escape ---
  var hamburger = document.querySelector(".hamburger");
  var sidebar = document.querySelector(".sidebar");
  var pantallaCompacta = window.matchMedia("(max-width: 1023px)");
  if (hamburger && sidebar) {
    var overlay = document.createElement("div");
    overlay.className = "sidebar-overlay";
    document.body.appendChild(overlay);
    var abrirSidebar = function (abrir) {
      sidebar.classList.toggle("abierto", abrir);
      overlay.classList.toggle("visible", abrir);
      hamburger.setAttribute("aria-expanded", abrir ? "true" : "false");
    };
    hamburger.addEventListener("click", function () { abrirSidebar(!sidebar.classList.contains("abierto")); });
    overlay.addEventListener("click", function () { abrirSidebar(false); });
    document.addEventListener("keydown", function (e) { if (e.key === "Escape") abrirSidebar(false); });
    sidebar.querySelectorAll("a").forEach(function (a) {
      a.addEventListener("click", function () { if (pantallaCompacta.matches) abrirSidebar(false); });
    });
    pantallaCompacta.addEventListener("change", function () { abrirSidebar(false); });
  }

  // --- Mantiene la posicion de scroll del sidebar entre navegaciones: al ser
  //     una app server-rendered, cada click recarga la pagina entera y el
  //     sidebar se recrea desde cero, perdiendo el scroll. Se guarda en
  //     sessionStorage (no localStorage: es solo para esta pestaña/sesion). ---
  if (sidebar) {
    var scrollGuardado = sessionStorage.getItem("sigai_sidebar_scroll");
    if (scrollGuardado) sidebar.scrollTop = parseInt(scrollGuardado, 10);
    sidebar.addEventListener("scroll", function () {
      sessionStorage.setItem("sigai_sidebar_scroll", sidebar.scrollTop);
    });
  }

  // --- Sidebar colapsado en desktop (seccion 4.4): icono + tooltip, persistido ---
  var btnColapsar = document.querySelector("#btn-colapsar-sidebar");
  if (btnColapsar && sidebar) {
    // El modo "solo iconos" es de escritorio: en celular/tablet el menu se desliza completo.
    var aplicarColapsado = function () {
      sidebar.classList.toggle("colapsado",
          !pantallaCompacta.matches && localStorage.getItem("sigai_sidebar_colapsado") === "true");
    };
    aplicarColapsado();
    pantallaCompacta.addEventListener("change", aplicarColapsado);
    sidebar.querySelectorAll(":scope > a, :scope > details > summary").forEach(function (item) {
      if (!item.title) item.title = item.textContent.trim();
    });
    btnColapsar.addEventListener("click", function () {
      var colapsado = sidebar.classList.toggle("colapsado");
      localStorage.setItem("sigai_sidebar_colapsado", colapsado ? "true" : "false");
    });
  }

  // --- Switch de tema claro/oscuro, persistido en localStorage ---
  var btnTema = document.querySelector("#btn-tema");
  var iconoTema = document.querySelector("#icono-tema");
  function aplicarIconoTema() {
    var oscuro = document.documentElement.getAttribute("data-theme") === "oscuro";
    if (iconoTema) iconoTema.textContent = oscuro ? "☀️" : "🌙";
    if (btnTema) btnTema.title = oscuro ? "Cambiar a tema claro" : "Cambiar a tema oscuro";
  }
  function alternarTema() {
    var oscuroActual = document.documentElement.getAttribute("data-theme") === "oscuro";
    if (oscuroActual) {
      document.documentElement.removeAttribute("data-theme");
      localStorage.setItem("sigai_tema", "claro");
    } else {
      document.documentElement.setAttribute("data-theme", "oscuro");
      localStorage.setItem("sigai_tema", "oscuro");
    }
    aplicarIconoTema();
  }
  if (btnTema) {
    aplicarIconoTema();
    btnTema.addEventListener("click", alternarTema);
  }
  // En celular/tablet el boton de tema esta dentro del menu del avatar
  document.querySelectorAll("[data-toggle-tema]").forEach(function (b) { b.addEventListener("click", alternarTema); });

  // --- Asistencia: modal de justificacion de inasistencia (seccion 5.2.8) ---
  var modalJustificar = document.querySelector("#modal-justificar");
  var inputJustificarId = document.querySelector("#input-justificar-asistencia-id");
  var btnCancelarJustificar = document.querySelector("#btn-cancelar-justificar");
  if (modalJustificar && inputJustificarId) {
    document.querySelectorAll("[data-justificar-id]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        inputJustificarId.value = btn.dataset.justificarId;
        modalJustificar.style.display = "flex";
      });
    });
    if (btnCancelarJustificar) {
      btnCancelarJustificar.addEventListener("click", function () { modalJustificar.style.display = "none"; });
    }
  }

  // --- Calificaciones: modal de edicion de evaluacion (seccion 5.3.4) ---
  var modalEditarEvaluacion = document.querySelector("#modal-editar-evaluacion");
  if (modalEditarEvaluacion) {
    document.querySelectorAll("[data-editar-evaluacion-id]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        document.querySelector("#editar-evaluacion-id").value = btn.dataset.editarEvaluacionId;
        document.querySelector("#editar-evaluacion-titulo").value = btn.dataset.editarEvaluacionTitulo;
        document.querySelector("#editar-evaluacion-tipo").value = btn.dataset.editarEvaluacionTipo;
        document.querySelector("#editar-evaluacion-fecha").value = btn.dataset.editarEvaluacionFecha;
        modalEditarEvaluacion.style.display = "flex";
      });
    });
    var btnCancelarEditarEvaluacion = document.querySelector("#btn-cancelar-editar-evaluacion");
    if (btnCancelarEditarEvaluacion) {
      btnCancelarEditarEvaluacion.addEventListener("click", function () { modalEditarEvaluacion.style.display = "none"; });
    }
  }

  // --- Horarios: modal de asignacion de slot (seccion 5.5.3) ---
  var modalEditarSlot = document.querySelector("#modal-editar-slot");
  if (modalEditarSlot) {
    var diasNombreSlot = ["", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes"];
    document.querySelectorAll("[data-editar-slot-dia]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        document.querySelector("#editar-slot-dia").value = btn.dataset.editarSlotDia;
        document.querySelector("#editar-slot-periodo").value = btn.dataset.editarSlotPeriodo;
        document.querySelector("#editar-slot-materia").value = btn.dataset.editarSlotMateria || "";
        document.querySelector("#editar-slot-docente").value = btn.dataset.editarSlotDocente || "";
        document.querySelector("#editar-slot-aula").value = btn.dataset.editarSlotAula || "";
        var descripcion = document.querySelector("#editar-slot-descripcion");
        if (descripcion) {
          descripcion.textContent = diasNombreSlot[btn.dataset.editarSlotDia] + ", periodo " + btn.dataset.editarSlotPeriodo;
        }
        modalEditarSlot.style.display = "flex";
      });
    });
    var btnCancelarEditarSlot = document.querySelector("#btn-cancelar-editar-slot");
    if (btnCancelarEditarSlot) {
      btnCancelarEditarSlot.addEventListener("click", function () { modalEditarSlot.style.display = "none"; });
    }
  }

  // --- Aulas: modal de reserva puntual (seccion 5.5.1) ---
  var modalReservarAula = document.querySelector("#modal-reservar-aula");
  if (modalReservarAula) {
    document.querySelectorAll("[data-reservar-aula-id]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        document.querySelector("#reservar-aula-id").value = btn.dataset.reservarAulaId;
        var nombre = document.querySelector("#reservar-aula-nombre");
        if (nombre) nombre.textContent = btn.dataset.reservarAulaNombre;
        modalReservarAula.style.display = "flex";
      });
    });
    var btnCancelarReservarAula = document.querySelector("#btn-cancelar-reservar-aula");
    if (btnCancelarReservarAula) {
      btnCancelarReservarAula.addEventListener("click", function () { modalReservarAula.style.display = "none"; });
    }
  }

  // --- Alumnos: recorte circular asistido de foto al subir (seccion 5.1.5/4.8) ---
  var inputFotoOriginal = document.querySelector("#input-foto-original");
  var canvasFoto = document.querySelector("#canvas-foto");
  var inputFotoRecortada = document.querySelector("#input-foto-recortada");
  var btnGuardarFoto = document.querySelector("#btn-guardar-foto");
  var formFoto = document.querySelector("#form-foto");
  if (inputFotoOriginal && canvasFoto) {
    var ctxFoto = canvasFoto.getContext("2d");
    inputFotoOriginal.addEventListener("change", function () {
      var archivo = inputFotoOriginal.files[0];
      if (!archivo) return;
      var img = new Image();
      img.onload = function () {
        var lado = Math.min(img.width, img.height);
        var sx = (img.width - lado) / 2;
        var sy = (img.height - lado) / 2;
        ctxFoto.clearRect(0, 0, canvasFoto.width, canvasFoto.height);
        ctxFoto.drawImage(img, sx, sy, lado, lado, 0, 0, canvasFoto.width, canvasFoto.height);
        btnGuardarFoto.disabled = false;
        URL.revokeObjectURL(img.src);
      };
      img.src = URL.createObjectURL(archivo);
    });
    formFoto.addEventListener("submit", function (evento) {
      if (inputFotoRecortada.dataset.listo === "true") return;
      evento.preventDefault();
      canvasFoto.toBlob(function (blob) {
        var archivo = new File([blob], "foto.jpg", { type: "image/jpeg" });
        var lista = new DataTransfer();
        lista.items.add(archivo);
        inputFotoRecortada.files = lista.files;
        inputFotoRecortada.dataset.listo = "true";
        formFoto.submit();
      }, "image/jpeg", 0.9);
    });
  }

  // --- Calificaciones: autoguardado de notas, indicador global (IMPLEMENTACION.md 3.5) ---
  document.querySelectorAll(".input-nota-autosave").forEach(function (input) {
    input.addEventListener("blur", function () {
      var valor = input.value.trim();
      var estadoGlobal = document.getElementById("estado-autoguardado-global");
      if (valor === "") return;
      var evaluacionId = document.getElementById("evaluacionId").value;
      var alumnoId = input.dataset.alumnoId;
      var csrf = document.getElementById("csrf-token");
      if (estadoGlobal) estadoGlobal.textContent = "Guardando...";
      var params = new URLSearchParams();
      params.set("evaluacionId", evaluacionId);
      params.set("alumnoId", alumnoId);
      params.set("valor", valor);
      params.set(csrf.name, csrf.value);
      fetch("/calificaciones/nota", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params.toString()
      }).then(function (r) { return r.json(); }).then(function (data) {
        if (!estadoGlobal) return;
        var ahora = new Date();
        var hora = String(ahora.getHours()).padStart(2, "0") + ":" + String(ahora.getMinutes()).padStart(2, "0");
        if (data.ok) {
          estadoGlobal.textContent = "Todo guardado · " + hora;
          input.classList.remove("error");
        } else {
          estadoGlobal.textContent = data.mensaje || "Error al guardar";
          input.classList.add("error");
        }
      }).catch(function () {
        if (estadoGlobal) estadoGlobal.textContent = "Error de conexion";
      });
    });
  });

  // --- Alumnos: "Seleccionar todos" para exportacion masiva (seccion 5.1.3) ---
  var chkTodos = document.querySelector("#chk-todos");
  if (chkTodos) {
    chkTodos.addEventListener("change", function () {
      document.querySelectorAll(".chk-alumno").forEach(function (chk) { chk.checked = chkTodos.checked; });
    });
  }

  // --- Dropdown de usuario en la topbar (seccion 4.4) ---
  var btnAvatar = document.querySelector("#btn-avatar");
  var dropdownUsuario = document.querySelector("#dropdown-usuario");
  if (btnAvatar && dropdownUsuario) {
    btnAvatar.addEventListener("click", function (evento) {
      evento.stopPropagation();
      dropdownUsuario.classList.toggle("abierto");
    });
    document.addEventListener("click", function (evento) {
      if (!dropdownUsuario.contains(evento.target)) {
        dropdownUsuario.classList.remove("abierto");
      }
    });
  }

  // --- Modales de confirmacion para acciones destructivas ---
  document.querySelectorAll("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (evento) {
      if (form.dataset.confirmado === "true") return;
      evento.preventDefault();
      abrirModalConfirmacion(form.dataset.confirm, form.dataset.confirmBoton, function () {
        form.dataset.confirmado = "true";
        form.submit();
      });
    });
  });

  // Igual, pero para un boton puntual de un formulario (los demas botones del form no preguntan).
  document.querySelectorAll("button[data-confirm]").forEach(function (boton) {
    boton.addEventListener("click", function (evento) {
      if (boton.dataset.confirmado === "true") {
        boton.dataset.confirmado = "";
        return;
      }
      evento.preventDefault();
      abrirModalConfirmacion(boton.dataset.confirm, boton.dataset.confirmBoton, function () {
        boton.dataset.confirmado = "true";
        boton.click();
      });
    });
  });

  function abrirModalConfirmacion(mensaje, textoBoton, onConfirmar) {
    var overlay = document.createElement("div");
    overlay.className = "modal-overlay";
    overlay.innerHTML =
      '<div class="modal-card">' +
      "<h3>Confirmar accion</h3>" +
      "<p>" + mensaje + "</p>" +
      '<div class="modal-actions">' +
      '<button type="button" class="btn btn-secondary" data-accion="cancelar">Cancelar</button>' +
      '<button type="button" class="btn btn-danger" data-accion="confirmar">' + (textoBoton || "Confirmar") + "</button>" +
      "</div></div>";
    document.body.appendChild(overlay);
    overlay.querySelector('[data-accion="cancelar"]').addEventListener("click", function () { overlay.remove(); });
    overlay.querySelector('[data-accion="confirmar"]').addEventListener("click", function () {
      overlay.remove();
      onConfirmar();
    });
    // No se cierra con click afuera ni Escape (seccion 4.7: evita cierres accidentales).
  }

  // --- Tabs (legajo del alumno, seccion 5.1.4) ---
  document.querySelectorAll(".tabs").forEach(function (tabs) {
    var links = tabs.querySelectorAll("a[data-tab]");
    var paneles = document.querySelectorAll("[data-tab-panel]");
    function activar(nombre) {
      links.forEach(function (l) { l.classList.toggle("activo", l.dataset.tab === nombre); });
      paneles.forEach(function (p) { p.style.display = p.dataset.tabPanel === nombre ? "" : "none"; });
    }
    links.forEach(function (link) {
      link.addEventListener("click", function (evento) {
        evento.preventDefault();
        activar(link.dataset.tab);
        history.replaceState(null, "", "#" + link.dataset.tab);
      });
    });
    var inicial = location.hash ? location.hash.substring(1) : (links[0] && links[0].dataset.tab);
    if (inicial) activar(inicial);
  });

  // --- Formulario de Alumno: agregar/quitar tutores (maximo 2, seccion 5.1.5) ---
  var tutoresContenedor = document.querySelector("#tutores-contenedor");
  var btnAgregarTutor = document.querySelector("#btn-agregar-tutor");
  if (tutoresContenedor && btnAgregarTutor) {
    btnAgregarTutor.addEventListener("click", function () {
      var filas = tutoresContenedor.querySelectorAll(".tutor-fieldset");
      if (filas.length >= 2) return;
      var indice = filas.length;
      var plantilla = document.querySelector("#tutor-template").innerHTML.replace(/__INDEX__/g, indice);
      var div = document.createElement("div");
      div.innerHTML = plantilla;
      tutoresContenedor.appendChild(div.firstElementChild);
      actualizarBotonAgregarTutor();
    });
    tutoresContenedor.addEventListener("click", function (evento) {
      if (evento.target.matches("[data-quitar-tutor]")) {
        evento.target.closest(".tutor-fieldset").remove();
        actualizarBotonAgregarTutor();
      }
    });
    function actualizarBotonAgregarTutor() {
      var cantidad = tutoresContenedor.querySelectorAll(".tutor-fieldset").length;
      btnAgregarTutor.disabled = cantidad >= 2;
    }
    actualizarBotonAgregarTutor();

    // --- Autocompletado de tutor por DNI (seccion 5.1.5): si el DNI ya
    //     existe en el sistema, autocompleta el resto de sus datos al salir
    //     del campo. "blur" no burbujea, por eso se escucha en fase de captura. ---
    tutoresContenedor.addEventListener("blur", function (evento) {
      if (!evento.target.matches("input[name$='.dni']")) return;
      var dni = evento.target.value.trim();
      if (dni.length < 7) return;
      fetch("/alumnos/tutores/buscar?dni=" + encodeURIComponent(dni))
        .then(function (r) { return r.json(); })
        .then(function (data) {
          if (!data.encontrado) return;
          var fieldset = evento.target.closest(".tutor-fieldset");
          var apellido = fieldset.querySelector("input[name$='.apellido']");
          var nombre = fieldset.querySelector("input[name$='.nombre']");
          var telefono = fieldset.querySelector("input[name$='.telefono']");
          var email = fieldset.querySelector("input[name$='.email']");
          if (apellido) apellido.value = data.apellido;
          if (nombre) nombre.value = data.nombre;
          if (telefono) telefono.value = data.telefono;
          if (email) email.value = data.email;
        })
        .catch(function () {});
    }, true);
  }

  // --- Formulario de Disciplina: agregar/quitar observaciones (sin limite, seccion 5.4.4) ---
  var observacionesContenedor = document.querySelector("#observaciones-contenedor");
  var btnAgregarObservacion = document.querySelector("#btn-agregar-observacion");
  if (observacionesContenedor && btnAgregarObservacion) {
    btnAgregarObservacion.addEventListener("click", function () {
      var indice = observacionesContenedor.querySelectorAll(".observacion-fieldset").length;
      var plantilla = document.querySelector("#observacion-template").innerHTML.replace(/__INDEX__/g, indice);
      var div = document.createElement("div");
      div.innerHTML = plantilla;
      observacionesContenedor.appendChild(div.firstElementChild);
      actualizarBotonesQuitarObservacion();
    });
    observacionesContenedor.addEventListener("click", function (evento) {
      if (evento.target.matches("[data-quitar-observacion]")) {
        evento.target.closest(".observacion-fieldset").remove();
        actualizarBotonesQuitarObservacion();
      }
    });
    function actualizarBotonesQuitarObservacion() {
      var filas = observacionesContenedor.querySelectorAll(".observacion-fieldset");
      filas.forEach(function (fila) {
        fila.querySelector("[data-quitar-observacion]").disabled = filas.length <= 1;
      });
    }
    actualizarBotonesQuitarObservacion();
  }

  // --- Educacion Fisica: filtro de alumnos por año y genero al armar un grupo (seccion 5.7.2) ---
  var listaAlumnosEf = document.querySelector("#lista-alumnos-ef");
  if (listaAlumnosEf) {
    var checksAnioEf = document.querySelectorAll(".filtro-anio-ef");
    var selectGeneroEf = document.querySelector("#filtro-genero-ef");
    var aplicarFiltroAlumnosEf = function () {
      var aniosSeleccionados = Array.prototype.filter.call(checksAnioEf, function (c) { return c.checked; })
        .map(function (c) { return c.value; });
      var genero = selectGeneroEf ? selectGeneroEf.value : "";
      listaAlumnosEf.querySelectorAll("[data-anio]").forEach(function (fila) {
        var coincideAnio = aniosSeleccionados.length === 0 || aniosSeleccionados.indexOf(fila.dataset.anio) !== -1;
        var coincideGenero = !genero || fila.dataset.genero === genero;
        fila.style.display = (coincideAnio && coincideGenero) ? "" : "none";
      });
    };
    checksAnioEf.forEach(function (c) { c.addEventListener("change", aplicarFiltroAlumnosEf); });
    if (selectGeneroEf) selectGeneroEf.addEventListener("change", aplicarFiltroAlumnosEf);
  }

  // --- Promocion de alumnos: al cambiar la accion, proponer el curso de destino que corresponde
  document.querySelectorAll(".accion-promocion").forEach(function (selectAccion) {
    selectAccion.addEventListener("change", function () {
      var destino = selectAccion.closest("tr").querySelector(".destino-promocion");
      if (!destino) return;
      if (selectAccion.value === "EGRESA") {
        destino.value = "";
        destino.disabled = true;
        return;
      }
      destino.disabled = false;
      var sugerido = selectAccion.value === "PASA" ? selectAccion.dataset.destinoPasa : selectAccion.dataset.destinoRepite;
      destino.value = sugerido || "";
    });
  });

  // --- Portal de padres: mostrar solo las notas de la materia elegida
  var selectorMateria = document.getElementById("selector-materia");
  if (selectorMateria) {
    selectorMateria.addEventListener("change", function () {
      document.querySelectorAll(".materia-detalle").forEach(function (bloque) {
        bloque.hidden = bloque.dataset.materia !== selectorMateria.value;
      });
    });
  }

  // --- Mostrar/ocultar contraseña: funciona con cualquier cantidad de campos
  // (login, cambiar contraseña, restablecer contraseña), cada boton apunta a
  // su input via data-target (IMPLEMENTACION.md 3.8) ---
  document.querySelectorAll(".btn-mostrar-password").forEach(function (btn) {
    var input = document.getElementById(btn.dataset.target);
    if (!input) return;
    btn.addEventListener("click", function () {
      var oculto = input.type === "password";
      input.type = oculto ? "text" : "password";
      btn.textContent = oculto ? btn.dataset.ocultar : btn.dataset.mostrar;
    });
  });

  // --- Asistencia: botones de estado por alumno (reemplazan el <select>, IMPLEMENTACION.md 3.2) ---
  (function () {
    var marcar = function (celda, tipo) {
      celda.querySelector(".input-tipo").value = tipo;
      celda.querySelectorAll(".btn-estado").forEach(function (b) { b.classList.toggle("activo", b.dataset.tipo === tipo); });
      var sel = celda.querySelector(".select-otros");
      if (sel) sel.value = ["UNIFORME_INCOMPLETO", "MEDIA_FALTA"].indexOf(tipo) !== -1 ? tipo : "";
      resumen();
    };
    var resumen = function () {
      var cont = document.getElementById("resumen-asistencia");
      if (!cont) return;
      var v = Array.prototype.map.call(document.querySelectorAll(".input-tipo"), function (i) { return i.value; });
      var n = function (t) { return v.filter(function (x) { return x === t; }).length; };
      cont.textContent = n("PRESENTE") + " presentes · " + n("AUSENTE") + " ausentes · " + n("LLEGADA_TARDE") + " tarde";
    };
    document.querySelectorAll(".btn-estado").forEach(function (b) {
      b.addEventListener("click", function () { marcar(b.closest("td"), b.dataset.tipo); });
    });
    document.querySelectorAll(".select-otros").forEach(function (s) {
      s.addEventListener("change", function () { if (s.value) marcar(s.closest("td"), s.value); });
    });
    var todos = document.getElementById("btn-todos-presentes");
    if (todos) {
      todos.addEventListener("click", function () {
        document.querySelectorAll("td .grupo-estado").forEach(function (g) { marcar(g.closest("td"), "PRESENTE"); });
      });
    }
    document.querySelectorAll(".input-tipo").forEach(function (i) { marcar(i.closest("td"), i.value); });
  })();

  // --- Formulario de Grupo de Educacion Fisica: agregar/quitar pruebas fisicas nuevas al catalogo (seccion 5.7.4) ---
  var definicionesContenedor = document.querySelector("#definiciones-nuevas-contenedor");
  var btnAgregarDefinicion = document.querySelector("#btn-agregar-definicion");
  if (definicionesContenedor && btnAgregarDefinicion) {
    btnAgregarDefinicion.addEventListener("click", function () {
      var indice = definicionesContenedor.querySelectorAll(".definicion-fieldset").length;
      var plantilla = document.querySelector("#definicion-nueva-template").innerHTML.replace(/__INDEX__/g, indice);
      var div = document.createElement("div");
      div.innerHTML = plantilla;
      definicionesContenedor.appendChild(div.firstElementChild);
    });
    definicionesContenedor.addEventListener("click", function (evento) {
      if (evento.target.matches("[data-quitar-definicion]")) {
        evento.target.closest(".definicion-fieldset").remove();
      }
    });
  }
});

// Mensajes: la conversacion abre mostrando el ultimo mensaje (abajo de todo)
document.addEventListener("DOMContentLoaded", function () {
  var hilo = document.getElementById("mensajes-hilo");
  if (hilo) hilo.scrollTop = hilo.scrollHeight;
});

// Horario semanal: pestañas de dia para celular (el CSS muestra solo el dia elegido).
// Arranca en el dia de hoy (lunes a viernes) y elegir un dia cambia todas las grillas de la pagina.
document.addEventListener("DOMContentLoaded", function () {
  var grillas = document.querySelectorAll("table.horario-semanal");
  if (!grillas.length) return;
  var hoy = new Date().getDay();
  var elegir = function (dia) {
    grillas.forEach(function (t) { t.setAttribute("data-dia", dia); });
    document.querySelectorAll(".dias-horario button").forEach(function (b) {
      b.classList.toggle("activo", b.dataset.dia === String(dia));
    });
  };
  grillas.forEach(function (tabla) {
    var encabezados = tabla.querySelectorAll("thead th");
    var barra = document.createElement("div");
    barra.className = "dias-horario";
    for (var d = 1; d <= 5 && d < encabezados.length; d++) {
      var b = document.createElement("button");
      b.type = "button";
      b.dataset.dia = String(d);
      b.textContent = encabezados[d].textContent.trim().slice(0, 3);
      b.title = encabezados[d].textContent.trim();
      b.addEventListener("click", function () { elegir(this.dataset.dia); });
      barra.appendChild(b);
    }
    var ancla = tabla.closest(".tabla-wrapper") || tabla;
    ancla.parentNode.insertBefore(barra, ancla);
  });
  elegir(hoy >= 1 && hoy <= 5 ? hoy : 1);
});

// Listas largas (ej. historial disciplinario en el portal de padres): muestra las primeras N
// filas y un boton "Ver todas (N)". Uso: <table data-mostrar-primeros="5" data-texto-ver-todas="...">
document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll("table[data-mostrar-primeros]").forEach(function (tabla) {
    var limite = parseInt(tabla.dataset.mostrarPrimeros, 10);
    var filas = tabla.querySelectorAll(":scope > tbody > tr");
    if (filas.length <= limite) return;
    for (var i = limite; i < filas.length; i++) filas[i].hidden = true;
    var boton = document.createElement("button");
    boton.type = "button";
    boton.className = "btn btn-ghost compacto ver-todas";
    boton.textContent = tabla.dataset.textoVerTodas || "Ver todas (" + filas.length + ")";
    boton.addEventListener("click", function () {
      filas.forEach(function (f) { f.hidden = false; });
      boton.remove();
    });
    var ancla = tabla.closest(".tabla-wrapper") || tabla;
    ancla.parentNode.insertBefore(boton, ancla.nextSibling);
  });
});
