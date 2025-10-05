SELECT base.doc_id,
ARRAY_AGG(distance order by distance ASC )   as distances ,
ARRAY_AGG(base.text order by distance ASC)  as text,
count(base.doc_id) as numberOfChucks,
ARRAY_AGG( distinct base.created_at)[offset(0)] as created_at
from 
VECTOR_SEARCH( (select * from `videoanalyzer-455321.text_embeddings_dataset.chunks`
where created_at in (select ARRAY_AGG( created_at 
order by created_at DESC LIMIT 1 )[offset(0)] 
from `videoanalyzer-455321.text_embeddings_dataset.chunks` 
where userid=@userId group by doc_id))
,"embedding",(SELECT @queryVector AS query_embedding) , top_k =>@topK,
distance_type =>'COSINE') group by base.doc_id order by min(distance) ASC