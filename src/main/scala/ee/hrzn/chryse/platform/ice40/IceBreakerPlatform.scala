package ee.hrzn.chryse.platform.ice40

import chisel3._
import ee.hrzn.chryse.platform.BoardPlatform
import ee.hrzn.chryse.platform.BoardResources
import ee.hrzn.chryse.platform.Platform
import ee.hrzn.chryse.platform.resource
import ee.hrzn.chryse.platform.resource.Pin._

final case class IceBreakerPlatform(ubtnReset: Boolean = false)
    extends BoardPlatform[IceBreakerResources] {
  val id      = "icebreaker"
  val clockHz = 12_000_000

  val nextpnrBinary = "nextpnr-ice40"
  val nextpnrArgs   = Seq("--up5k", "--package", "sg48")
  val packBinary    = "icepack"
  val programBinary = "iceprog"

  val resources = new IceBreakerResources

  override def apply[Top <: Module](genTop: Platform => Top) =
    ICE40Top(this, genTop(this))
}

class IceBreakerResources extends BoardResources {
  val clock = resource.ClockSource(12_000_000).onPin(35)

  val ubtn = resource.Button().inverted.onPin(10)

  val uart = resource.UART().onPins(rx = 6, tx = 9)

  val ledg = resource.LED().inverted.onPin(37)
  val ledr = resource.LED().inverted.onPin(11)

  val pmod1a1  = resource.InOut().onPin(4)
  val pmod1a2  = resource.InOut().onPin(2)
  val pmod1a3  = resource.InOut().onPin(47)
  val pmod1a4  = resource.InOut().onPin(45)
  val pmod1a7  = resource.InOut().onPin(3)
  val pmod1a8  = resource.InOut().onPin(48)
  val pmod1a9  = resource.InOut().onPin(46)
  val pmod1a10 = resource.InOut().onPin(44)

  val pmod1b1  = resource.InOut().onPin(43)
  val pmod1b2  = resource.InOut().onPin(38)
  val pmod1b3  = resource.InOut().onPin(34)
  val pmod1b4  = resource.InOut().onPin(31)
  val pmod1b7  = resource.InOut().onPin(42)
  val pmod1b8  = resource.InOut().onPin(36)
  val pmod1b9  = resource.InOut().onPin(32)
  val pmod1b10 = resource.InOut().onPin(28)

  val pmod2_1  = resource.InOut().onPin(27)
  val pmod2_2  = resource.InOut().onPin(25)
  val pmod2_3  = resource.InOut().onPin(21)
  val pmod2_4  = resource.InOut().onPin(19)
  val pmod2_7  = resource.InOut().onPin(26)
  val pmod2_8  = resource.InOut().onPin(23)
  val pmod2_9  = resource.InOut().onPin(20)
  val pmod2_10 = resource.InOut().onPin(18)
}
