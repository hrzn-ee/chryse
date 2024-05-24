package ee.hrzn.chryse.platform.resource

import chisel3._

case class ClockSource(hz: Int) extends ResourceData[Clock](Input(Clock())) {}

object ClockSource {
  def apply(hz: Int) = new ClockSource(hz)
}