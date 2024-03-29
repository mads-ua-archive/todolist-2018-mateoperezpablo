package controllers;

import java.util.ArrayList;

import models.Equipo;
import models.Usuario;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import security.ActionAuthenticator;
import services.EquipoService;
import services.UsuarioService;

import javax.persistence.Entity;

import play.Logger;

// Es necesario importar las vistas que se van a usar
import services.EquipoServiceException;
import views.html.formNuevoEquipo;
import views.html.listaEquipos;
import views.html.formEquipoUsuario;
import views.html.listaEquiposUsuario;
import views.html.detalleEquipo;
import views.html.listaEquiposAdministrador;
import views.html.detalleEquipoAdministrador;

import javax.inject.Inject;
import java.util.List;

public class EquipoController extends Controller {
    @Inject
    FormFactory formFactory;
    @Inject
    EquipoService equipoService;
    @Inject UsuarioService usuarioService;

    @Security.Authenticated(ActionAuthenticator.class)
    public Result formularioNuevoEquipo() {
        return ok(formNuevoEquipo.render(""));
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result creaNuevoEquipo() {
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String nombre = requestData.get("nombre");
        if (nombre == null || nombre.equals("")) {
            return badRequest(formNuevoEquipo.render("Debes rellenar el nombre"));
        }
        equipoService.addEquipo(nombre);
        //return ok("Equipo " + nombre + " añadido correctamente");
        return redirect(routes.EquipoController.listaEquiposAdministrador());
    }

    public Result listaEquipos() {
        List<Equipo> equipos = equipoService.allEquipos();
        return ok(listaEquipos.render(equipos));
    }

    public Result listaEquiposAdministrador() {
        String connectedUserStr = session("connected");
        if(connectedUserStr==null) return unauthorized("Lo siento, no estás autorizado");
        Long connectedUser =  Long.valueOf(connectedUserStr);
        Usuario usuario = usuarioService.findUsuarioPorId(connectedUser);
        if(!usuario.getAdministrador()) return unauthorized("Lo siento, no estás autorizado");

        List<Equipo> equipos = equipoService.allEquipos();

        return ok(listaEquiposAdministrador.render(equipos, usuario));
    }

    public Result borrarEquiposAdministrador(Long id) {
        String connectedUserStr = session("connected");
        if(connectedUserStr==null) return unauthorized("Lo siento, no estás autorizado");
        Long connectedUser =  Long.valueOf(connectedUserStr);
        Usuario usuario = usuarioService.findUsuarioPorId(connectedUser);
        if(!usuario.getAdministrador()) return unauthorized("Lo siento, no estás autorizado");

        Equipo equipo = equipoService.findById(id);
        equipoService.delete(equipo);
        return redirect(routes.EquipoController.listaEquiposAdministrador());
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result listaEquiposUsuario(Long id) {
        String connectedUserStr = session("connected");
        Long connectedUser =  Long.valueOf(connectedUserStr);
        if (!connectedUser.equals(id)) {
            return unauthorized("Lo siento, no estás autorizado");
        } else {
            Usuario usuario = usuarioService.findUsuarioPorId(id);
            List<Equipo> equipos = new ArrayList<Equipo>(usuario.getEquipos());
            return ok(listaEquiposUsuario.render(equipos, usuario));
        }
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result detalleEquipoAdministrador(Long id){
        String connectedUserStr = session("connected");
        if(connectedUserStr==null) return unauthorized("Lo siento, no estás autorizado");
        Long connectedUser =  Long.valueOf(connectedUserStr);
        Usuario usuario = usuarioService.findUsuarioPorId(connectedUser);
        if(!usuario.getAdministrador()) return unauthorized("Lo siento, no estás autorizado");

        Equipo equipo = equipoService.findById(id);
        List<Usuario> usuarios = new ArrayList<Usuario>(equipoService.findUsuariosEquipo(equipo.getNombre()));

        List<Usuario> nousu = new ArrayList<Usuario>(equipoService.findUsuariosNoEquipo(equipo.getNombre()));

        Logger.debug(" prueba "+nousu.size());

        return ok(detalleEquipoAdministrador.render(usuario, equipo, usuarios, nousu));
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result detalleEquipo(Long id){
        String connectedUserStr = session("connected");
        if(connectedUserStr==null) return unauthorized("Lo siento, no estás autorizado");
        Long connectedUser =  Long.valueOf(connectedUserStr);
        Usuario usuario = usuarioService.findUsuarioPorId(connectedUser);
        List<Equipo> equipos = new ArrayList<Equipo>(usuario.getEquipos());
        for(int i=0;i<equipos.size();i++){
            Equipo p = equipos.get(i);
            Logger.debug("Iterando" + p.getId() + " " + id);
            if(p.getId().equals(id)){
                Logger.debug("FOUND" + p.getId() + " " + id);
                List<Usuario> usu = new ArrayList<Usuario>(equipoService.findUsuariosEquipo(p.getNombre()));

                return ok(detalleEquipo.render(usuario, p, usu));
            }
        }
        return unauthorized("Lo siento, no estás autorizado");
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result formularioAddUsuarioEquipo() {
        return ok(formEquipoUsuario.render());
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result addUsuarioEquipo() {
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String equipo = requestData.get("equipo");
        String usuario = requestData.get("usuario");
        try {
            equipoService.addUsuarioEquipo(usuario, equipo);
            return ok("Usuario " + usuario + " añadido al equipo " + equipo);
        } catch (EquipoServiceException exception) {
            return notFound("No existe usuario / equipo");
        }
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result deleteUsuarioEquipo(Long usuarioId, Long equipoId){
        String connectedUserStr = session("connected");
        if(connectedUserStr==null) return unauthorized("Lo siento, no estás autorizado");
        Long connectedUser =  Long.valueOf(connectedUserStr);
        Usuario usuario = usuarioService.findUsuarioPorId(connectedUser);
        if(!usuario.getAdministrador()) return unauthorized("Lo siento, no estás autorizado");
 
        Usuario usu = usuarioService.findUsuarioPorId(usuarioId);

        Equipo equi = equipoService.findById(equipoId);

        equipoService.deleteUsuarioEquipo(usu.getLogin(), equi.getNombre());

        return redirect(routes.EquipoController.detalleEquipoAdministrador(equipoId));
    }

    @Security.Authenticated(ActionAuthenticator.class)
    public Result addUsuarioEquipoAdministrador(Long equipoId){
        String connectedUserStr = session("connected");
        if(connectedUserStr==null) return unauthorized("Lo siento, no estás autorizado");
        Long connectedUser =  Long.valueOf(connectedUserStr);
        Usuario usuario = usuarioService.findUsuarioPorId(connectedUser);
        if(!usuario.getAdministrador()) return unauthorized("Lo siento, no estás autorizado");
 
        //Usuario usu = usuarioService.findUsuarioPorId(usuarioId);

        DynamicForm requestData = formFactory.form().bindFromRequest();
        String idS = requestData.get("usuarioInput");

        Equipo equi = equipoService.findById(equipoId);
        

        equipoService.addUsuarioEquipo(idS, equi.getNombre());

        return redirect(routes.EquipoController.detalleEquipoAdministrador(equipoId));
    }
}
