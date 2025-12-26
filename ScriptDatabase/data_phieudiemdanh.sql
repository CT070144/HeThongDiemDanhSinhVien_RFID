 select
        pdd1_0.id,
        pdd1_0.ca,
        pdd1_0.created_at,
        pdd1_0.giora,
        pdd1_0.giovao,
        pdd1_0.masinhvien,
        pdd1_0.ngay,
        pdd1_0.phonghoc,
        pdd1_0.rfid,
        pdd1_0.tensinhvien,
        pdd1_0.tinhtrangdiemdanh,
        pdd1_0.trangthai,
        pdd1_0.updated_at 
    from
        phieudiemdanh pdd1_0 
    where
        pdd1_0.phonghoc='603-TA1'
        and pdd1_0.ngay='2025-08-21'
        and pdd1_0.ca=1
    order by
        pdd1_0.giovao