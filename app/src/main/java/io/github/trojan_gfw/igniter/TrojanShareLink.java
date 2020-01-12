package io.github.trojan_gfw.igniter;

public class TrojanShareLink {
    public static String GenerateShareLink(String remoteAddress, String remotePort, String password) {
        return "trojan://" + password + "@" + remoteAddress + ":" + remotePort;
    }

    public static String[] ConvertShareToTrojanConf(String trojanShareLink) {
        if (trojanShareLink.startsWith("trojan://")) {
            String[] tmp = new String[3];
            String tsl = trojanShareLink.substring(9);
            String[] temp = tsl.split(":");
            tmp[1] = temp[temp.length - 1].split("#")[0];
            try
            {
                Integer.parseInt(tmp[1]);
                temp[temp.length - 1] = "";
            }
            catch (Exception ex) {
                return  null;
            }

            tsl = CombineToString(temp);
            String[] temp_1 = tsl.split("@");
            tmp[0] = temp_1[temp_1.length - 1];
            temp_1[temp_1.length - 1] = "";
            tmp[2] = CombineToString(temp_1);
            return  tmp;
        }else
            return null;
    }

    public static String CombineToString(String[] str) {
        String tmp = "";
        for(String s : str) {
            tmp += s;
        }
        return tmp;
    }
}
