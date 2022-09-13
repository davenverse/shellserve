package io.chrisdavenport.shellserve

import cats.effect._
import cats.syntax.all._
import cats.effect.std._
import org.http4s._
import io.chrisdavenport.process._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._

object Shellserve extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val cp = ChildProcess.impl[IO]
    args.toNel.fold(Console[IO].errorln("No Command Provided please give a shell command").as(ExitCode.Error)){args =>
      val proc = Process(args.head, args.tail)
      EmberServerBuilder.default[IO]
        .withHttp2
        .withHost(host"127.0.0.1") // TODO make these configurable
        .withPort(port"9093")
        .withHttpApp(route(cp, proc))
        .build
        .useForever
        .as(ExitCode.Success)
    }
  }

  def route(cp: ChildProcess[IO], proc: Process) = HttpRoutes.of[IO]{
    case _ =>
      cp.exec(proc).map(out => Response[IO](Status.Ok).withEntity(out.trim()))
  }.orNotFound

}