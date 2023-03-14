package gemmini

import chisel3._

object Direction extends Enumeration {
  type Direction = Value
  val Forward, Backward, Bidirectional, Disabled = Value
  // Forward is defined as Left to Right and and Top to Bottom. In the case of Anti-diagonal the Left to Right takes precedence.
}

case class ChannelConfig[T <: Data](
  channelDirection : Direction.Value = Direction.Disabled,
  channelType : T,
  channelCount : Int = 0
)

case class InterconnectConfig[V <: Data, H <: Data, D <: Data, A <: Data](
  VerticalDirection : Direction.Value = Direction.Disabled,
  VerticalType : V = Bits(0.W),
  VerticalCount : Int = 0, 
  HorizontalDirection : Direction.Value = Direction.Disabled,
  HorizontalType : H = Bits(0.W),
  HorizontalCount : Int = 0, 
  DiagonalDirection : Direction.Value = Direction.Disabled,
  DiagonalType : D = Bits(0.W),
  DiagonalCount : Int = 0, 
  AntiDiagonalDirection : Direction.Value = Direction.Disabled,
  AntiDiagonalType : A = Bits(0.W),
  AntiDiagonalCount : Int = 0) 
  {
    val Vertical = ChannelConfig(VerticalDirection, VerticalType, VerticalCount)
    val Horizontal = ChannelConfig(HorizontalDirection, HorizontalType, HorizontalCount)
    val Diagonal = ChannelConfig(VerticalDirection, DiagonalType, DiagonalCount)
    val AntiDiagonal = ChannelConfig(AntiDiagonalDirection, AntiDiagonalType, AntiDiagonalCount)
  }

object CommonInterconnectConfigs {
  // Dataflow direction of the unmodified Gemmini
  val OrthogonalForwad = InterconnectConfig(
    VerticalDirection = Direction.Forward, 
    VerticalType = SInt(32.W),
    VerticalCount = 2,
    HorizontalDirection = Direction.Forward, 
    HorizontalType = SInt(8.W),
    HorizontalCount = 1
  )

  val OrthogonalAndDiagonalForward = InterconnectConfig(
    VerticalDirection = Direction.Forward, 
    HorizontalDirection = Direction.Forward, 
    DiagonalDirection = Direction.Forward, 
    VerticalType = SInt(8.W), 
    HorizontalType = SInt(8.W),
    DiagonalType = SInt(8.W)
    )
  // val AllForward = InterconnectConfig(Vertical = Direction.Forward, Horizontal = Direction.Forward, Diagonal = Direction.Forward, AntiDiagonal = Direction.Forward)
  // val AllBidirectional = InterconnectConfig(Vertical = Direction.Bidirectional, Horizontal = Direction.Bidirectional, Diagonal = Direction.Bidirectional, AntiDiagonal = Direction.Bidirectional)
  // val OrthogonalBidirectional = InterconnectConfig(Vertical = Direction.Bidirectional, Horizontal = Direction.Bidirectional)
}