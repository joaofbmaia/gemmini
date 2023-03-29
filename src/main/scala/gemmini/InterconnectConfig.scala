package gemmini

import chisel3._

case class InterconnectConfig[T <: Data : Arithmetic](
  verticalBroadcastType : T,
  horizontalBroadcastType : T,
  interPEType : T
)

object CommonInterconnectConfigs {
  // Dataflow direction of the unmodified Gemmini
  val DefaultICConfig = InterconnectConfig[SInt](
    verticalBroadcastType = SInt(32.W),
    horizontalBroadcastType = SInt(8.W),
    interPEType = SInt(32.W)
  )
}