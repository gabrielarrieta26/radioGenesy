/* Estado de reproducción compartido entre el estudio y el oyente. */
(function (global) {
    "use strict";

    function crear(config) {
        const audio = config.audio;
        const endpoint = config.endpoint;
        const rol = config.rol;
        const urlDeRecurso = config.urlDeRecurso;
        const mostrar = config.mostrar;
        const setBoton = config.setBoton;
        const onPista = config.onPista;
        const onError = config.onError;

        let version = -1;
        let idAudio = null;
        let aplicando = false;
        let esperando = false;
        let estado = null;
        let esperandoGesto = false;
        let gestoRegistrado = false;

        function avisar(mensaje) {
            if (onError) onError(mensaje);
        }

        function esAudible(estadoRemoto) {
            return !!(estadoRemoto && estadoRemoto.sonido === rol && !esperandoGesto);
        }

        function aplicarSilencio(estadoRemoto) {
            const silenciar = !esAudible(estadoRemoto);
            if (audio.muted !== silenciar) audio.muted = silenciar;
        }

        function esperarPrimerGesto() {
            if (gestoRegistrado) return;
            gestoRegistrado = true;
            const activar = function () {
                esperandoGesto = false;
                aplicarSilencio(estado);
                document.removeEventListener("pointerdown", activar, true);
                document.removeEventListener("keydown", activar, true);
                gestoRegistrado = false;
            };
            document.addEventListener("pointerdown", activar, true);
            document.addEventListener("keydown", activar, true);
        }

        function reproducirAudio() {
            const resultado = audio.play();
            if (resultado && typeof resultado.catch === "function") {
                resultado.catch(function () {
                    if (esperandoGesto) return;
                    esperandoGesto = true;
                    audio.muted = true;
                    const reintento = audio.play();
                    if (reintento && typeof reintento.catch === "function") {
                        reintento.catch(function () { setBoton(false); });
                    }
                    esperarPrimerGesto();
                    avisar("Radio sincronizada. Hacé un clic para activar el sonido.");
                });
            }
        }

        function aplicar(estadoRemoto) {
            if (!estadoRemoto) return;
            estado = estadoRemoto;
            aplicarSilencio(estadoRemoto);
            const pista = estadoRemoto.pista;

            if (!pista) {
                aplicando = true;
                audio.pause();
                audio.removeAttribute("src");
                audio.load();
                idAudio = null;
                mostrar(null);
                setBoton(false);
                version = estadoRemoto.version;
                setTimeout(function () { aplicando = false; }, 300);
                return;
            }

            if (idAudio !== pista.id) {
                aplicando = true;
                esperando = true;
                audio.src = urlDeRecurso(pista.urlAudio);
                audio.load();
                idAudio = pista.id;

                function listo() {
                    if (!esperando) return;
                    esperando = false;
                    aplicando = false;
                    aplicarSilencio(estado);
                    if (estadoRemoto.posicion > 0) {
                        try { audio.currentTime = estadoRemoto.posicion; } catch (error) { /* sin metadata */ }
                    }
                    if (estadoRemoto.reproduciendo) reproducirAudio();
                    else setBoton(false);
                }
                audio.addEventListener("loadedmetadata", listo, { once: true });
                audio.addEventListener("error", listo, { once: true });
                setTimeout(listo, 3000);

                if (onPista) onPista(pista);
            } else {
                if (audio.readyState >= 1 && estadoRemoto.posicion >= 0
                        && Math.abs(audio.currentTime - estadoRemoto.posicion) > 2) {
                    try { audio.currentTime = estadoRemoto.posicion; } catch (error) { /* sin metadata */ }
                }
                if (estadoRemoto.reproduciendo && audio.paused) reproducirAudio();
                if (!estadoRemoto.reproduciendo && !audio.paused) {
                    aplicando = true;
                    audio.pause();
                    setTimeout(function () { aplicando = false; }, 300);
                }
            }

            mostrar(pista);
            setBoton(estadoRemoto.reproduciendo);
            version = estadoRemoto.version;
        }

        async function enviar(ruta) {
            const respuesta = await fetch(endpoint + ruta, { method: "POST" });
            if (!respuesta.ok) throw new Error("No se pudo actualizar la reproducción.");
            return respuesta.json();
        }

        async function consultar() {
            try {
                const respuesta = await fetch(endpoint + "?rol=" + encodeURIComponent(rol));
                if (respuesta.ok) aplicar(await respuesta.json());
            } catch (error) {
                avisar("Sin conexión con el estudio.");
            }
        }

        return {
            iniciar: function () {
                consultar();
                setInterval(consultar, 1000);
            },
            consultar: consultar,
            aplicar: aplicar,
            reproducir: async function (id) {
                await enviar("/reproducir/" + id);
                await consultar();
            },
            pausar: async function () {
                await enviar("/pausar");
                await consultar();
            },
            reanudar: async function () {
                await enviar("/reanudar");
                await consultar();
            },
            alternar: async function (primeraId) {
                if (!estado) await consultar();
                if (estado && estado.reproduciendo) await enviar("/pausar");
                else if (estado && estado.pista) await enviar("/reanudar");
                else if (primeraId != null) await enviar("/reproducir/" + primeraId);
                await consultar();
            },
            siguiente: async function () {
                await enviar("/siguiente?version=" + version);
                await consultar();
            },
            fijarPosicion: async function (segundos) {
                await enviar("/posicion?segundos=" + segundos);
                await consultar();
            },
            esAplicando: function () { return aplicando; },
            esAudible: function () { return esAudible(estado); },
            estaEsperandoGesto: function () { return esperandoGesto; },
            activarSonido: function () {
                esperandoGesto = false;
                aplicarSilencio(estado);
            },
            esReproduciendo: function () { return !!(estado && estado.reproduciendo); },
            obtenerEstado: function () { return estado; }
        };
    }

    global.ReproductorSync = { crear: crear };
})(window);
