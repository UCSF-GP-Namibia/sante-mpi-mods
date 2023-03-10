package bmt;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList; 
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.base64.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
 
import org.json.JSONObject;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap; 
import java.util.Map;
import java.util.UUID;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import ca.uhn.fhir.context.FhirContext; 
import ca.uhn.fhir.model.dstu2.resource.Patient; 
import ca.uhn.fhir.model.dstu2.valueset.AddressUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum; 
import ca.uhn.fhir.model.primitive.DateDt;

public class issueHealthId {

    private final static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    public static HttpResponse<String> authenticateClient(String app) throws Exception {

        String client_id = "";
        String secret = "";
        // form parameters
        
        if (app.equals("EDT"))
        {  
            client_id =  "EDT";
            secret = "***";
        }
        else if (app.equals("PTracker"))
        {
            client_id = "PTracker";
            secret = "***";
        }
        else if (app.equals("Quantum"))
        {
            client_id = "Quantum";
            secret = "***";
        }

        Map<Object, Object> data;
        data = new HashMap<>();
        data.put("grant_type", "client_credentials");
        data.put("client_id", client_id);
        data.put("client_secret", secret);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(buildFormDataFromMap(data))
                .uri(URI.create("http://localhost:8080/auth/oauth2_token"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


        // print status code
        System.out.println(response.statusCode());

        // print response body
        System.out.println(response.body());
        return response;

    }

    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        System.out.println(builder.toString());
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    private static String uuidToBase64(String uuid) {
        ByteBuffer uuidBuffer = ByteBuffer.allocate(16);
        LongBuffer longBuffer = uuidBuffer.asLongBuffer();
        longBuffer.put(UUID.fromString(uuid).getMostSignificantBits());
        longBuffer.put(UUID.fromString(uuid).getLeastSignificantBits());

        String encoded = new String(Base64.encode(uuidBuffer.array()), Charset.forName("US-ASCII"));

        return encoded;
    }

    private static String uuidFromBase64(String str) {
        byte[] bytes = Base64.decode(str);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid.toString();

    }


    public static void main( String[] args ) throws Exception
    {
        String db_url = "jdbc:postgresql://localhost:5432/santedb";
        String user = "santedb";
        String password = "***";

        try (Connection con = DriverManager.getConnection(db_url, user, password);
                
                Statement st_guid_ident = con.createStatement();
                ResultSet rs_guid_ident = st_guid_ident.executeQuery("SELECT distinct src_ent_id, trg_ent_id, dob, UPPER(gn.val), tel_val , cd.val, sat.app_pub_id AS source_app FROM public.ent_rel_tbl er inner join public.ent_vrsn_tbl ev ON er.src_ent_id = ev.ent_id inner join public.psn_tbl ps on  ev.ent_vrsn_id = ps.ent_vrsn_id left join public.cd_name_tbl cd  on er.cls_cd_id = cd.cd_id and cd.obslt_vrsn_seq_id is null left join public.cd_name_tbl gn ON gn.cd_id = gndr_cd_id left join public.ent_tel_tbl tel on tel.ent_id = er.src_ent_id left join ent_vrsn_tbl evt on evt.ent_id=src_ent_id left join sec_prov_tbl spt ON evt.crt_prov_id = spt.prov_id left join sec_app_tbl sat on sat.app_id = spt.app_id WHERE er.obslt_vrsn_seq_id is null and ev.rplc_vrsn_id IS NULL ORDER BY cd.val ASC")) 
                {

                    while(rs_guid_ident.next())
                    {
                        String src_uuid = rs_guid_ident.getString(1);
                        String trg_uuid = rs_guid_ident.getString(2);
                        System.out.println(trg_uuid);

                        String dob = rs_guid_ident.getString(3);
                        String gender = rs_guid_ident.getString(4).toUpperCase();
                        String tel = rs_guid_ident.getString(5);
                        String cl = rs_guid_ident.getString(6);
                        String app = rs_guid_ident.getString(7);

                        ArrayList<identifier> identifiers_list = getIdentifiers(trg_uuid, con);

                        ArrayList<address> add_list = getAddress(trg_uuid, con);

                        Patient ourPatient = new Patient();
                        ourPatient.setId(src_uuid);
                        
                        String link = "http://127.0.0.1:8080/fhir/Patient/"+src_uuid; 

                        System.out.println(cl);
                        System.out.println(link);

                        String encoded_uuid = uuidToBase64(trg_uuid);
                        
                        FhirContext ctx = FhirContext.forDstu2();

                        ourPatient.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue(tel); 
                        ourPatient.setGender(AdministrativeGenderEnum.valueOf(gender)); 

                        DateDt dt = new DateDt(dob); 
                        ourPatient.setBirthDate(dt);

                        String country = "";
                        String state = "";
                        String address = "";

                        for(address a : add_list){

                            
                            if (a.uuid.equals(src_uuid))
                            {
                                if (a.add_type.contains("country")){
                                    country = a.add_val;
                                }
    
                                if (a.add_type.contains("state")){
                                    state = a.add_val;
                                }
    
                                if (a.add_type.contains("PostalCode")){
                                    address = a.add_val;
                                }  
                            }                               
                        }
                        ourPatient.addAddress().setUse(AddressUseEnum.HOME).setCountry(country).setState(state).setPostalCode(address);

 
                        for(identifier i : identifiers_list){
                            
                            if (i.uuid.equals(src_uuid))
                            {  
                                ourPatient.addIdentifier().setSystem(i.ident_name).setValue(i.ident_val);
                            }
                            
                        }
                        
                        ourPatient.addIdentifier().setSystem("http://ohie.org/Health_ID").setValue(encoded_uuid.replace("=", ""));

                        String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(ourPatient);

                        System.out.println(encoded);

                        HttpResponse<String> auth_response = authenticateClient(app);
                        JSONObject token = new JSONObject(auth_response.body());
                        
                        String accessToken = (String) token.get("access_token"); 
                        
                        String auth = "Bearer " + accessToken;

                        postResource(link, encoded, auth);
 
                    }

                } catch (SQLException ex) {
                
                    Logger lgr = Logger.getLogger(issueHealthId.class.getName());
                    lgr.log(Level.SEVERE, ex.getMessage(), ex);
                }   

    }

    private static void postResource(String link, String encoded, String auth)
            throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(link);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("PUT");
        http.setDoOutput(true);
        http.setRequestProperty("Authorization", auth);
        http.setRequestProperty("Accept", "text/html,application/fhir+xml,application/xml;q=0.9,*/*;q=0.8");
               
        http.setRequestProperty("Accept", "application/fhir+json");
        http.setRequestProperty("Content-Type", "application/fhir+json");

        String data = encoded;

        byte[] out = data.getBytes(StandardCharsets.UTF_8);

        OutputStream stream = http.getOutputStream();
        stream.write(out);

        System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
        http.disconnect();
    }

    private static ArrayList<address> getAddress(String mp, Connection con) throws SQLException {
        String sql_add = 
        "select * from ( SELECT DISTINCT src_ent_id,cd.val as add_type, ent_add_val.val, ent_add_val.val_seq_id, trg_ent_id, row_number() over (partition by src_ent_id, cd.val order by ent_add_val.val_seq_id) as rnum from public.ent_addr_tbl ent_add inner join public.ent_rel_tbl er on ent_add.ent_id = er.src_ent_id  left join public.ent_addr_cmp_tbl ent_add_comp on ent_add_comp.addr_id = ent_add.addr_id  left join public.ent_addr_cmp_val_tbl ent_add_val on  ent_add_val.val_seq_id = ent_add_comp.val_seq_id  LEFT JOIN public.cd_name_tbl cd ON cd.cd_id = ent_add_comp.typ_cd_id ) a where a.rnum = 1 and a.trg_ent_id = '"+mp+"' ORDER BY a.val_seq_id DESC";


        Statement st_add = con.createStatement();
        ResultSet rs_add = st_add.executeQuery(sql_add);
        ArrayList<address> add_list = new ArrayList<address>();
        while(rs_add.next())
        {
            String uuid = rs_add.getString(1);
            String add_type = rs_add.getString(2);
            String add_val = rs_add.getString(3);
           
            add_list.add(new address(uuid, add_type, add_val));   
        }
        return add_list;
    }

    private static ArrayList<identifier> getIdentifiers(String mp, Connection con) throws SQLException {
        String sql_ident = "SELECT DISTINCT src_ent_id,url, id_val FROM public.ent_id_tbl  ent inner join public.ent_rel_tbl er on ent.ent_id = er.src_ent_id inner JOIN public.asgn_aut_tbl asg on ent.aut_id=asg.aut_id WHERE trg_ent_id = '" + mp+ "'  and ent.obslt_vrsn_seq_id is null";

        Statement st_ident = con.createStatement();
        ResultSet rs_ident = st_ident.executeQuery(sql_ident);
        ArrayList<identifier> identifiers_list = new ArrayList<identifier>();
        while(rs_ident.next())
        {
            String uuid = rs_ident.getString(1);
            String ident_name = rs_ident.getString(2);
            String ident_val = rs_ident.getString(3);
           
            if (!ident_name.contains("Health_ID"))
            {
                identifiers_list.add(new identifier(uuid, ident_name, ident_val));   
            }
            
        }
        return identifiers_list;
    }
}
