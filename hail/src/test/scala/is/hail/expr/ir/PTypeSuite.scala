package is.hail.expr.ir

import is.hail.HailSuite
import is.hail.expr.types.physical._
import is.hail.expr.types.virtual
import is.hail.expr.types.virtual.{TArray, TDict, TInt32, TInterval, TStruct}
import is.hail.rvd.AbstractRVDSpec
import is.hail.utils._
import is.hail.variant.ReferenceGenome
import org.apache.spark.sql.Row
import org.json4s.jackson.Serialization
import org.testng.annotations.{DataProvider, Test}

class PTypeSuite extends HailSuite {

  @DataProvider(name="ptypes")
  def ptypes(): Array[Array[Any]] = {
    Array[PType](
      PInt32(true),
      PInt32(false),
      PInt64(true),
      PInt64(false),
      PFloat32(true),
      PFloat64(true),
      PBoolean(true),
      PCanonicalCall(true),
      PCanonicalBinary(false),
      PCanonicalString(true),
      PCanonicalLocus(ReferenceGenome.GRCh37, false),
      PCanonicalArray(PInt32Required, true),
      PCanonicalSet(PInt32Required, false),
      PCanonicalDict(PInt32Required, PCanonicalString(true), true),
      PCanonicalInterval(PInt32Optional, false),
      PCanonicalTuple(FastIndexedSeq(PTupleField(1, PInt32Required), PTupleField(3, PCanonicalString(false))), true),
      PCanonicalStruct(FastIndexedSeq(PField("foo", PInt32Required, 0), PField("bar", PCanonicalString(false), 1)), true)
    ).map(t => Array(t: Any))
  }

  @Test def testPTypesDataProvider(): Unit = {
    ptypes()
  }

  @Test(dataProvider="ptypes")
  def testSerialization(ptype: PType): Unit = {
    implicit val formats = AbstractRVDSpec.formats
    val s = Serialization.write(ptype)
    assert(Serialization.read[PType](s) == ptype)
  }

  @Test def testLiteralPType(): Unit = {
    assert(PType.literalPType(TInt32, 5) == PInt32(true))
    assert(PType.literalPType(TInt32, null) == PInt32())

    assert(PType.literalPType(TArray(TInt32), null) == PCanonicalArray(PInt32(true)))
    assert(PType.literalPType(TArray(TInt32), FastIndexedSeq(1, null)) == PCanonicalArray(PInt32(), true))
    assert(PType.literalPType(TArray(TInt32), FastIndexedSeq(1, 5)) == PCanonicalArray(PInt32(true), true))

    assert(PType.literalPType(TInterval(TInt32), Interval(5, null, false, true)) == PCanonicalInterval(PInt32(), true))

    val p = TStruct("a" -> TInt32, "b" -> TInt32)
    val d = TDict(p, p)
    assert(PType.literalPType(d, Map(Row(3, null) -> Row(null, 3))) ==
      PCanonicalDict(
        PCanonicalStruct(true, "a" -> PInt32(true), "b" -> PInt32()),
        PCanonicalStruct(true, "a" -> PInt32(), "b" -> PInt32(true)),
        true))
  }
}
