<?php
include('db_con.php');
header('Content-type:application/json');
include('token.php');
$user_name_friend = $_REQUEST["username"];
$mysql_qry = "select user_id,user_username from users where user_username = ? and user_status_id = '1'";
// $result = mysqli_query($conn ,$mysql_qry);
$result = mysqli_prepare($conn ,$mysql_qry);
mysqli_stmt_bind_param($result,'s',$user_name_friend);
mysqli_stmt_execute($result);
$result= mysqli_stmt_get_result($result);
$row=mysqli_fetch_array($result);
$ret = array();
if($row){
    $ret['status']="success";
    
    $ret['message']=$user_name_friend;
}
else{
    $ret['status']="fail";
    $ret['message']="Not found";
}
echo json_encode($ret);
?>