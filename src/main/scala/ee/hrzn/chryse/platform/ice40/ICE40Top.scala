package ee.hrzn.chryse.platform.ice40

import chisel3._
import chisel3.experimental.IntParam
import chisel3.experimental.StringParam
import chisel3.experimental.noPrefix
import chisel3.util._
import chisel3.util.experimental.forceName
import ee.hrzn.chryse.chisel.DirectionOf
import ee.hrzn.chryse.platform.ChryseTop
import ee.hrzn.chryse.platform.PlatformBoard
import ee.hrzn.chryse.platform.PlatformBoardResources
import ee.hrzn.chryse.platform.resource

import scala.collection.mutable

class ICE40Top[Top <: Module](
    platform: PlatformBoard[_ <: PlatformBoardResources],
    genTop: => Top,
) extends RawModule
    with ChryseTop {
  override protected def platformConnect(
      name: String,
      res: resource.ResourceData[_ <: Data],
  ): Option[(Data, Data)] = {
    if (name == "ubtn" && ubtn_reset.isDefined) {
      if (res.ioInst.isDefined)
        throw new Exception("ubtnReset requested but ubtn used in design")

      // XXX: do we need to bother hooking up res.topIoInst/portIoInst?

      val topIo = Wire(res.makeIo()).suggestName("ubtn_top")
      ubtn_reset.get := topIo

      val portIo = IO(res.makeIo()).suggestName("ubtn")
      return Some((topIo, portIo))
    }
    None
  }

  override protected def platformPort[HW <: Data](
      res: resource.ResourceData[HW],
      topIo: Data,
      portIo: Data,
  ) = {
    // when we do DDR we'll need to use PIN_INPUT_DDR.
    val i_type = PinType.PIN_INPUT

    // as above, PIN_OUTPUT_{REGISTERED,DDR}_ENABLE_REGISTERED
    val o_type = DirectionOf(portIo) match {
      case DirectionOf.Input  => PinType.PIN_NO_OUTPUT
      case DirectionOf.Output => PinType.PIN_OUTPUT_TRISTATE
    }

    val buffer = Module(
      new SB_IO(
        i_type | o_type,
        res.attributes("IO_STANDARD").asInstanceOf[StringParam].value,
        res.attributes
          .get("PULLUP")
          .map(_.asInstanceOf[IntParam].value == 1)
          .getOrElse(false),
        gen = res.makeIo(),
      ),
    ).suggestName(s"${res.name.get}_SB_IO")

    DirectionOf(portIo) match {
      case DirectionOf.Input =>
        buffer.PACKAGE_PIN   := portIo
        topIo                := buffer.D_IN_0
        buffer.OUTPUT_ENABLE := DontCare
        buffer.D_OUT_0       := DontCare
      case DirectionOf.Output =>
        portIo               := buffer.PACKAGE_PIN
        buffer.OUTPUT_ENABLE := true.B
        buffer.D_OUT_0       := topIo
    }
  }

  private val clki = Wire(Clock())

  private val clk_gb = Module(new SB_GB_IO)
  clk_gb.PACKAGE_PIN := clki
  private val clk = clk_gb.GLOBAL_BUFFER_OUTPUT

  private val timerLimit = (15e-6 * platform.clockHz).toInt
  private val resetTimerReg =
    withClock(clk)(Reg(UInt(unsignedBitLength(timerLimit).W)))
  private val reset = Wire(Bool())

  when(resetTimerReg === timerLimit.U) {
    reset := false.B
  }.otherwise {
    reset         := true.B
    resetTimerReg := resetTimerReg + 1.U
  }

  var ubtn_reset: Option[Bool] = None
  private val finalReset = noPrefix {
    if (platform.asInstanceOf[IceBreakerPlatform].ubtnReset) {
      ubtn_reset = Some(Wire(Bool()))
      reset | ~ubtn_reset.get
    } else {
      reset
    }
  }

  private val top =
    withClockAndReset(clk, finalReset)(Module(genTop))
  if (top.desiredName == desiredName)
    throw new IllegalArgumentException(s"user top is called $desiredName")

  // TODO (iCE40): allow clock source override.

  private val connectedResources =
    connectResources(platform, Some(clki))

  val lastPCF = Some(
    PCF(
      connectedResources
        .map { case (name, cr) => (name, cr.pin) }
        .to(Map),
      connectedResources
        .flatMap { case (name, cr) => cr.frequencyHz.map((name, _)) }
        .to(Map),
    ),
  )
}
