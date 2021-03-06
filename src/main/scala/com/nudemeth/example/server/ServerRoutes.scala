package com.nudemeth.example.server

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Source,Sink}


import com.nudemeth.example.engine._
import com.nudemeth.example.viewmodel._
import spray.json._


trait ServerRoutes extends JsonSupport {
  implicit def system: ActorSystem

  def log: LoggingAdapter = Logging(system, this.getClass)

  lazy val route: Route = concat(
    home,
    about,
    js,
    dataHome,
    dataAbout,
  )

  private lazy val nashorn: JavaScriptEngine = new NashornEngine(
    Seq(
      ScriptURL(getClass.getResource("/webapp/js/polyfill/nashorn-polyfill.js")),
      ScriptURL(getClass.getResource("/webapp/js/bundle.js")),
      ScriptText("var frontend = new com.nudemeth.example.web.Frontend();")
    )
  )

  private val home: Route = {
    pathEndOrSingleSlash {
      get {
        val model = HomeViewModel("This is Home page").toJson.compactPrint
        val content = nashorn.invokeMethod[String]("frontend", "renderServer", "/", model)
        val html = views.html.index.render(content, model).toString()
        //val logTest1 = CassandraRequest.logValeurs()
        log.info(s"Request: route=/, method=get")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
      }
    }
  }

  private val about: Route = {
    path("about") {
      pathEndOrSingleSlash {
        get {
          val model = AboutViewModel("About page").toJson.compactPrint
          val content = nashorn.invokeMethod[String]("frontend", "renderServer", "/about", model)
          val html = views.html.index.render(content, model).toString()
          log.info(s"Request: route=/, method=get")
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
        }
      }
    } ~
    path("pairs") {
      pathEndOrSingleSlash {
        post {
          entity(as[String]) { param =>

            val ingArray = param.parseJson.convertTo[IngredientsModel]
            // Requete Cassandra here

            val model = """{ "pairs": [["Banana","Sel","0"],["Sel","Patate","1"]] }""".parseJson.compactPrint
            log.info(s"Request: route=/, method=post")
            complete(HttpEntity(ContentTypes.`application/json`, model))
          }
        }
      }
    }
  }


  private val dataHome: Route = {
    path("data" / "home") {
      pathEndOrSingleSlash {
        get {
          val model = HomeViewModel("This is Home page").toJson.compactPrint
          log.info(s"Request: route=/, method=get")
          complete(HttpEntity(ContentTypes.`application/json`, model))
        }
      }
    }
  }

  private val dataAbout: Route = {
    path("data" / "about") {
      pathEndOrSingleSlash {
        get {
          val model = AboutViewModel("About page").toJson.compactPrint
          log.info(s"Request: route=/, method=get")
          complete(HttpEntity(ContentTypes.`application/json`, model))
        }
      }
    }
  }

  private val js: Route = {
    get {
      pathPrefix("js" / Segment) { file =>
        log.info(s"Request: route=/js/$file, method=get")
        val js = scala.io.Source.fromURL(getClass.getResource(s"/webapp/js/$file"))("UTF-8").mkString
        complete(HttpEntity(MediaTypes.`application/javascript` withCharset HttpCharsets.`UTF-8`, js))
      }
    }
  }

}
