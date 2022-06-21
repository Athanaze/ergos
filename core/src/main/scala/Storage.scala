import java.nio.file.NoSuchFileException
import scala.:+
import scala.collection.mutable.ListBuffer

object Storage {
  val storagePath = os.pwd / "wallets.ergos"

  def addWallet(title: String, mnemonic: String) = {
    try {
      val currentFile = os.read(storagePath)
      var contentToWrite = ""
      if(currentFile == ""){
        contentToWrite = title+"\n"+mnemonic
      }else{
        contentToWrite = currentFile+"\n"+title+"\n"+mnemonic
      }
      if (os.remove(storagePath)){
        os.write(storagePath, contentToWrite)
      }
    }
    catch {
      case e: NoSuchFileException => {
        os.write(storagePath, "default_wallet\n"+ BIP39.genMnemonic())
      }
    }
  }

  def getListWallets(): List[ErgosWallet]= {
    var walletName = ""
    var mnemonic = ""
    val lb = new ListBuffer[ErgosWallet]()
    os.read(storagePath).split("\n").iterator.foreach(fValue => {
      if(walletName == ""){
        walletName = fValue
      }else{
        mnemonic = fValue
      }

      if(walletName != "" & mnemonic !="" ){
        lb.addOne(ErgosWallet(walletName, mnemonic))
        walletName = ""
        mnemonic = ""
      }
    })

    lb.toList
  }
}
