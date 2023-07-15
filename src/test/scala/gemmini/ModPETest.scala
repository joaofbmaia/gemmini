package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chisel3.stage.ChiselStage
import scala.util.Random

class ModPETest extends AnyFlatSpec with ChiselScalatestTester{
  (new ChiselStage).emitVerilog(new ModPE(CommonInterconnectConfigs.DefaultICConfig))

  val rand = new scala.util.Random

  val A = rand.nextInt(255) - 128
  val B = rand.nextInt()
  val C = rand.nextInt()
  val ACC_REG = rand.nextInt()
  val A_alt = rand.nextInt(255) - 128
  val B_alt = rand.nextInt()
  val ACC_REG_alt = rand.nextInt()

  it should "MUL" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.ZERO)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(A * B)
    }
  }

  it should "ADD" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.ZERO)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.ADD)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(A + B)
    }
  }

  it should "SUB" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.ZERO)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.SUB)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(A - B)
    }
  }

  it should "MADD3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(A * B + C)
    }
  }

  it should "MSUB3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.SUB)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(A * B - C)
    }
  }

  it should "SADD3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.SUB)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(A - B + C)
    }
  }

  it should "NMADD3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(true)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(-(A * B) + C)
    }
  }

  it should "NMSUB3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.SUB)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(true)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(-(A * B) - C)
    }
  }

  it should "MADD2" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(0.S)
      dut.io.in_h_grid.poke(0.S)
      dut.io.in_v.poke(ACC_REG)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.REG)
      dut.io.control.sel_q.poke(PEMuxSel.V)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.REG)
      dut.io.control.double_buffer_sel.poke(0.U)
      dut.io.valid.poke(true)

      dut.clock.step(1)

      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(ACC_REG_alt)

      dut.io.control.double_buffer_sel.poke(1.U)

      dut.clock.step(1)

      dut.io.in_v_grid.poke(B_alt)
      dut.io.in_h_grid.poke(A_alt)
      dut.io.in_v.poke(0.S)

      dut.io.control.double_buffer_sel.poke(0.U)
      dut.io.out.expect(A * B + ACC_REG)

      dut.clock.step(1)

      dut.io.control.double_buffer_sel.poke(1.U)
      dut.io.out.expect(A_alt * B_alt + ACC_REG_alt)
    }
  }

  it should "MMADD2" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(0.S)
      dut.io.in_h_grid.poke(0.S)
      dut.io.in_v.poke(ACC_REG)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.REG)
      dut.io.control.sel_q.poke(PEMuxSel.V)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.ADD)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.BIG_ALU)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(true)

      dut.io.control.sel_out.poke(OutMuxSel.REG)
      dut.io.control.double_buffer_sel.poke(0.U)
      dut.io.valid.poke(true)

      dut.clock.step(1)

      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(ACC_REG_alt)

      dut.io.control.double_buffer_sel.poke(1.U)

      dut.clock.step(1)

      dut.io.in_v_grid.poke(B_alt)
      dut.io.in_h_grid.poke(A_alt)
      dut.io.in_v.poke(0.S)

      dut.io.control.double_buffer_sel.poke(0.U)
      dut.io.out.expect(-(A * B) + ACC_REG)

      dut.clock.step(1)

      dut.io.control.double_buffer_sel.poke(1.U)
      dut.io.out.expect(-(A_alt * B_alt) + ACC_REG_alt)
    }
  }

  it should "MAX3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.MAX3)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.A)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(if (A > B) A else C)
    }
  }

  it should "MIN3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.MIN3)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.A)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(if (A < B) A else C)
    }
  }

  it should "EQ3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.EQ3)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.A)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(if (A == B) A else C)
    }
  }

  it should "NEQ3" in {
    test(new ModPE(CommonInterconnectConfigs.DefaultICConfig)) { dut =>
      dut.io.in_v_grid.poke(B)
      dut.io.in_h_grid.poke(A)
      dut.io.in_v.poke(0.S)
      dut.io.in_h.poke(0.S)
      dut.io.in_d.poke(C)

      dut.io.control.sel_a.poke(PEMuxSel.H_GRID)
      dut.io.control.sel_b.poke(PEMuxSel.V_GRID)
      dut.io.control.sel_c.poke(PEMuxSel.D)

      dut.io.control.fu_control.shift.poke(0.U)
      dut.io.control.fu_control.big_alu_sel.poke(BigALUSel.MUL)
      dut.io.control.fu_control.small_alu_sel.poke(SmallALUSel.NEQ3)
      dut.io.control.fu_control.sel_m.poke(MMuxSel.A)
      dut.io.control.fu_control.minus_a.poke(false)
      dut.io.control.fu_control.minus_m.poke(false)

      dut.io.control.sel_out.poke(OutMuxSel.FU)

      dut.io.out.expect(if (A != B) A else C)
    }
  }
}
